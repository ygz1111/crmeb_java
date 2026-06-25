# -*- coding: utf-8 -*-
"""
转享 App 品牌资源生成脚本
生成: logo、tabBar 图标(8个)、默认头像
配色: 活力橙金 #FF6B35 / #FFB627
依赖: Pillow
"""
import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

OUT_DIR = os.path.dirname(os.path.abspath(__file__))
TABBAR_DIR = os.path.join(OUT_DIR, "tabbar")
os.makedirs(TABBAR_DIR, exist_ok=True)

# ===== 色板 =====
PRIMARY = (255, 107, 53)      # #FF6B35 暖橙
ACCENT = (255, 182, 39)       # #FFB627 金黄
GRAY = (156, 163, 175)        # #9CA3AF 灰态
WHITE = (255, 255, 255)
DARK = (26, 26, 46)           # #1A1A2E

FONT_BOLD = r"C:\Windows\Fonts\msyhbd.ttc"   # 微软雅黑粗体
FONT_HEI = r"C:\Windows\Fonts\simhei.ttf"    # 黑体


def load_font(path, size):
    return ImageFont.truetype(path, size)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def gradient(size, c1, c2, angle_deg=135):
    """生成对角渐变图"""
    w, h = size
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    px = img.load()
    rad = math.radians(angle_deg)
    dx, dy = math.cos(rad), math.sin(rad)
    # 投影长度归一化
    diag = abs(dx) * w + abs(dy) * h
    if diag == 0:
        diag = 1
    for y in range(h):
        for x in range(w):
            t = (x * dx + y * dy)
            # 归一到 0..1（以左上角为 0）
            t = (t + (abs(dx) * w + abs(dy) * h) / 2) / (abs(dx) * w + abs(dy) * h)
            t = max(0.0, min(1.0, t))
            px[x, y] = lerp(c1, c2, t) + (255,)
    return img


def rounded_rect_mask(size, radius):
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255)
    return mask


# ============================================================
# 1. Logo  (456 x 180)  图标 + 文字「转享」
# ============================================================
def make_logo():
    W, H = 456, 180
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # ---- 左侧图标徽章: 圆角方块 + 循环箭头「转」 ----
    badge = 132
    bx, by = 16, (H - badge) // 2
    # 渐变圆角底
    grad = gradient((badge, badge), PRIMARY, ACCENT, 135)
    mask = rounded_rect_mask((badge, badge), 34)
    img.paste(grad, (bx, by), mask)

    # 在徽章中心绘制循环箭头(两个半圆箭头组成循环符号)
    bd = ImageDraw.Draw(img)
    cx, cy = bx + badge // 2, by + badge // 2
    r = 34
    lw = 9
    # 上半弧 (从左到右，箭头朝右下)
    bd.arc([cx - r, cy - r - 6, cx + r, cy + r - 6], start=320, end=170, fill=WHITE, width=lw)
    # 下半弧 (从右到左，箭头朝左上)
    bd.arc([cx - r, cy - r + 6, cx + r, cy + r + 6], start=140, end=-10, fill=WHITE, width=lw)
    # 两个箭头三角
    # 右下箭头头
    bd.polygon([(cx + r + 2, cy - 4), (cx + r + 16, cy + 6), (cx + r - 2, cy + 14)], fill=WHITE)
    # 左上箭头头
    bd.polygon([(cx - r - 2, cy + 4), (cx - r - 16, cy - 6), (cx - r + 2, cy - 14)], fill=WHITE)

    # ---- 右侧文字「转享」----
    text_x = bx + badge + 26
    f_title = load_font(FONT_HEI, 78)
    f_sub = load_font(FONT_BOLD, 26)
    d.text((text_x, 30), "转享", font=f_title, fill=DARK)
    # 副标题
    d.text((text_x + 4, 118), "闲置好物 · 循环流转", font=f_sub, fill=lerp(GRAY, DARK, 0.2))

    img.save(os.path.join(OUT_DIR, "logo.png"))
    print("✓ logo.png")


# ============================================================
# 2. tabBar 图标 (81x81)  线性风格，统一描边
# ============================================================
def new_canvas():
    return Image.new("RGBA", (81, 81), (0, 0, 0, 0))


def draw_home(d, color, s=6):
    """首页: 房屋"""
    w = 7
    # 屋顶
    d.line([(40, 16), (16, 38)], fill=color, width=w)
    d.line([(40, 16), (64, 38)], fill=color, width=w)
    # 主体
    d.line([(20, 35), (20, 62)], fill=color, width=w)
    d.line([(60, 35), (60, 62)], fill=color, width=w)
    d.line([(16, 38), (64, 38)], fill=color, width=w)
    d.line([(20, 62), (60, 62)], fill=color, width=w)
    # 门
    d.rectangle([35, 46, 46, 62], outline=color, width=w - 1)


def draw_cate(d, color):
    """分类: 四宫格"""
    w = 6
    gap = 6
    cell = 22
    x0, y0 = 14, 14
    cells = [
        (x0, y0, x0 + cell, y0 + cell),
        (x0 + cell + gap, y0, x0 + 2 * cell + gap, y0 + cell),
        (x0, y0 + cell + gap, x0 + cell, y0 + 2 * cell + gap),
        (x0 + cell + gap, y0 + cell + gap, x0 + 2 * cell + gap, y0 + 2 * cell + gap),
    ]
    for c in cells:
        d.rounded_rectangle(c, radius=6, outline=color, width=w)


def draw_cart(d, color):
    """购物车"""
    w = 6
    # 车把到车身
    d.line([(16, 22), (20, 22)], fill=color, width=w + 1)
    d.line([(20, 22), (26, 48)], fill=color, width=w)
    d.line([(26, 48), (60, 48)], fill=color, width=w)
    d.line([(60, 48), (66, 28)], fill=color, width=w)
    d.line([(26, 28), (66, 28)], fill=color, width=w)
    # 轮子
    d.ellipse([24, 54, 36, 66], outline=color, width=w - 1)
    d.ellipse([52, 54, 64, 66], outline=color, width=w - 1)


def draw_publish(d, color):
    """发布: 圆角方块 + 加号"""
    w = 6
    d.rounded_rectangle([14, 14, 67, 67], radius=16, outline=color, width=w)
    cx = cy = 40
    d.line([(cx - 14, cy), (cx + 14, cy)], fill=color, width=w + 1)
    d.line([(cx, cy - 14), (cx, cy + 14)], fill=color, width=w + 1)


def draw_user(d, color):
    """我的: 人头"""
    w = 6
    # 头
    d.ellipse([30, 16, 51, 37], outline=color, width=w)
    # 身(半圆)
    d.arc([18, 38, 63, 72], start=180, end=360, fill=color, width=w)
    d.line([(18, 72), (63, 72)], fill=color, width=w)


def make_tabbar():
    specs = [
        ("home", draw_home),
        ("cate", draw_cate),
        ("cart", draw_cart),
        ("publish", draw_publish),
        ("user", draw_user),
    ]
    for name, fn in specs:
        # 灰态
        c = new_canvas()
        fn(ImageDraw.Draw(c), GRAY)
        c.save(os.path.join(TABBAR_DIR, f"{name}wei.png"))
        # 选中态
        c = new_canvas()
        fn(ImageDraw.Draw(c), PRIMARY)
        c.save(os.path.join(TABBAR_DIR, f"{name}xuan.png"))
        print(f"✓ tabbar/{name}wei.png  {name}xuan.png")


# ============================================================
# 3. 默认头像 (120x120)  渐变底 + 循环符号
# ============================================================
def make_avatar():
    S = 240  # 2x 生成后不缩放，直接用
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    grad = gradient((S, S), PRIMARY, ACCENT, 135)
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, S - 1, S - 1], fill=255)
    img.paste(grad, (0, 0), mask)

    d = ImageDraw.Draw(img)
    cx, cy = S // 2, S // 2
    r = 52
    lw = 14
    d.arc([cx - r, cy - r - 9, cx + r, cy + r - 9], start=320, end=170, fill=WHITE, width=lw)
    d.arc([cx - r, cy - r + 9, cx + r, cy + r + 9], start=140, end=-10, fill=WHITE, width=lw)
    d.polygon([(cx + r + 3, cy - 6), (cx + r + 24, cy + 9), (cx + r - 3, cy + 21)], fill=WHITE)
    d.polygon([(cx - r - 3, cy + 6), (cx - r - 24, cy - 9), (cx - r + 3, cy - 21)], fill=WHITE)

    img = img.resize((120, 120), Image.LANCZOS)
    img.save(os.path.join(OUT_DIR, "avatar_default.png"))
    print("✓ avatar_default.png")


if __name__ == "__main__":
    print("生成品牌资源到:", OUT_DIR)
    make_logo()
    make_tabbar()
    make_avatar()
    print("\n全部完成。")
