import request from '@/utils/request.js';

export function publishSecondHand(data) {
    return request.post('secondhand/publish', data);
}

export function updateSecondHand(data) {
    return request.post('secondhand/update', data);
}

export function getMySecondHandList(page, limit) {
    return request.get('secondhand/mylist', {
        page: page || 1,
        limit: limit || 10
    });
}

export function deleteSecondHand(id) {
    return request.post('secondhand/delete/' + id);
}

export function getSecondHandList(page, limit) {
    return request.get('secondhand/list', {
        page: page || 1,
        limit: limit || 10
    }, { noAuth: true });
}

export function getMyPublishStats() {
    return request.get('secondhand/stats');
}
