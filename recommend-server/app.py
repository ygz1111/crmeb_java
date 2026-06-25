import os
import pymysql
import numpy as np
from flask import Flask, request, jsonify
from flask_cors import CORS
from dotenv import load_dotenv
from sklearn.metrics.pairwise import cosine_similarity
from collections import defaultdict
import time

load_dotenv()

app = Flask(__name__)
CORS(app)

DB_CONFIG = {
    'host': os.getenv('DB_HOST', '127.0.0.1'),
    'port': int(os.getenv('DB_PORT', 3306)),
    'user': os.getenv('DB_USER', 'root'),
    'password': os.getenv('DB_PASSWORD', 'crmeb123456'),
    'database': os.getenv('DB_NAME', 'single_open'),
    'charset': 'utf8mb4',
}

_cache = {'matrix': None, 'user_ids': None, 'product_ids': None, 'ts': 0}
CACHE_TTL = 300


def get_db():
    return pymysql.connect(**DB_CONFIG, cursorclass=pymysql.cursors.DictCursor)


def build_interaction_matrix():
    now = time.time()
    if _cache['matrix'] is not None and (now - _cache['ts']) < CACHE_TTL:
        return _cache['matrix'], _cache['user_ids'], _cache['product_ids']

    conn = get_db()
    try:
        with conn.cursor() as cur:
            cur.execute("""
                SELECT uid, product_id, type FROM eb_store_product_relation
                WHERE type IN ('collect', 'like')
            """)
            relations = cur.fetchall()

            cur.execute("""
                SELECT o.uid, oi.product_id FROM eb_store_order_info oi
                JOIN eb_store_order o ON oi.order_id = o.id
                WHERE o.paid = 1 AND o.is_del = 0
            """)
            orders = cur.fetchall()

            cur.execute("""
                SELECT uid, product_id FROM eb_store_cart
                WHERE status != 11
            """)
            carts = cur.fetchall()

            cur.execute("""
                SELECT id, store_name, image, price, cate_id, sales, browse,
                       is_second_hand, ai_category
                FROM eb_store_product
                WHERE is_del = 0 AND is_show = 1
            """)
            products = cur.fetchall()
    finally:
        conn.close()

    user_weights = {'collect': 3.0, 'like': 1.0, 'order': 5.0, 'cart': 2.0}

    interactions = defaultdict(lambda: defaultdict(float))
    for r in relations:
        interactions[r['uid']][r['product_id']] += user_weights.get(r['type'], 1.0)
    for o in orders:
        interactions[o['uid']][o['product_id']] += user_weights['order']
    for c in carts:
        interactions[c['uid']][c['product_id']] += user_weights['cart']

    user_ids = sorted(interactions.keys())
    product_ids = sorted(set(pid for uid_map in interactions.values() for pid in uid_map))

    if not user_ids or not product_ids:
        _cache.update({'matrix': np.array([]), 'user_ids': [], 'product_ids': [], 'ts': now})
        return np.array([]), [], []

    uid_index = {uid: i for i, uid in enumerate(user_ids)}
    pid_index = {pid: i for i, pid in enumerate(product_ids)}

    matrix = np.zeros((len(user_ids), len(product_ids)))
    for uid, pid_map in interactions.items():
        for pid, score in pid_map.items():
            matrix[uid_index[uid]][pid_index[pid]] = score

    _cache.update({'matrix': matrix, 'user_ids': user_ids, 'product_ids': product_ids, 'ts': now})
    return matrix, user_ids, product_ids


def get_recommendations(target_uid, top_n=10):
    matrix, user_ids, product_ids = build_interaction_matrix()

    if matrix.size == 0 or target_uid not in user_ids:
        return get_popular_products(top_n)

    target_idx = user_ids.index(target_uid)
    user_sim = cosine_similarity(matrix[target_idx:target_idx+1], matrix)[0]
    user_sim[target_idx] = -1

    top_sim_indices = np.argsort(user_sim)[-10:][::-1]
    target_vector = matrix[target_idx]

    scores = np.zeros(len(product_ids))
    sim_sum = 0
    for sim_idx in top_sim_indices:
        if user_sim[sim_idx] <= 0:
            continue
        sim_sum += user_sim[sim_idx]
        diff = matrix[sim_idx] - target_vector
        scores += user_sim[sim_idx] * diff

    if sim_sum > 0:
        scores /= sim_sum

    scores[target_vector > 0] = -1

    rec_indices = np.argsort(scores)[-top_n:][::-1]
    rec_product_ids = [product_ids[i] for i in rec_indices if scores[i] > 0]

    if not rec_product_ids:
        return get_popular_products(top_n)

    return fetch_products(rec_product_ids)


def get_popular_products(top_n=10):
    conn = get_db()
    try:
        with conn.cursor() as cur:
            cur.execute("""
                SELECT id, store_name, image, price, cate_id, sales, browse,
                       is_second_hand, ai_category
                FROM eb_store_product
                WHERE is_del = 0 AND is_show = 1
                ORDER BY (sales * 3 + browse) DESC
                LIMIT %s
            """, (top_n,))
            return cur.fetchall()
    finally:
        conn.close()


def fetch_products(product_ids):
    if not product_ids:
        return []
    conn = get_db()
    try:
        placeholders = ','.join(['%s'] * len(product_ids))
        with conn.cursor() as cur:
            cur.execute(f"""
                SELECT id, store_name, image, price, cate_id, sales, browse,
                       is_second_hand, ai_category
                FROM eb_store_product
                WHERE id IN ({placeholders}) AND is_del = 0 AND is_show = 1
            """, product_ids)
            rows = {r['id']: r for r in cur.fetchall()}
        return [rows[pid] for pid in product_ids if pid in rows]
    finally:
        conn.close()


@app.route('/api/recommend', methods=['GET'])
def recommend():
    uid = request.args.get('uid', type=int)
    top_n = request.args.get('n', default=10, type=int)

    try:
        if uid and uid > 0:
            items = get_recommendations(uid, top_n)
        else:
            items = get_popular_products(top_n)
        return jsonify({'code': 200, 'msg': 'success', 'data': items})
    except Exception as e:
        print(f'[RECOMMEND ERROR] {e}')
        return jsonify({'code': 500, 'msg': str(e), 'data': []})


@app.route('/api/recommend/similar/<int:product_id>', methods=['GET'])
def similar_products(product_id):
    top_n = request.args.get('n', default=6, type=int)
    try:
        conn = get_db()
        with conn.cursor() as cur:
            cur.execute("""
                SELECT id, store_name, image, price, cate_id, sales, browse,
                       is_second_hand, ai_category
                FROM eb_store_product
                WHERE id = %s AND is_del = 0
            """, (product_id,))
            target = cur.fetchone()

            if not target:
                return jsonify({'code': 404, 'msg': 'product not found', 'data': []})

            cur.execute("""
                SELECT id, store_name, image, price, cate_id, sales, browse,
                       is_second_hand, ai_category
                FROM eb_store_product
                WHERE id != %s AND is_del = 0 AND is_show = 1
                  AND (cate_id = %s OR ai_category = %s)
                ORDER BY (sales * 3 + browse) DESC
                LIMIT %s
            """, (product_id, target['cate_id'], target['ai_category'], top_n))
            results = cur.fetchall()
        conn.close()
        return jsonify({'code': 200, 'msg': 'success', 'data': results})
    except Exception as e:
        return jsonify({'code': 500, 'msg': str(e), 'data': []})


@app.route('/api/recommend/refresh', methods=['POST'])
def refresh_cache():
    _cache.update({'matrix': None, 'ts': 0})
    return jsonify({'code': 200, 'msg': 'cache refreshed'})


@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'ok', 'service': 'CRMEB Recommend Server'})


if __name__ == '__main__':
    port = int(os.getenv('RECOMMEND_PORT', 5000))
    print(f'\nRecommend Server started on port {port}')
    app.run(host='0.0.0.0', port=port, debug=False)
