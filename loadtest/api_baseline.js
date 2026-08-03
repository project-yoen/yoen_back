// YOEN API 응답시간 측정 시나리오
// 사전 조건: seed_users.js 실행 → seed.sql 실행 → 앱 기동
// 실행: k6 run loadtest/api_baseline.js
// 옵션: k6 run -e BASE_URL=http://localhost:8080 -e VUS=10 -e DURATION=2m loadtest/api_baseline.js
import http from 'k6/http';
import { check, group } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const VUS = Number(__ENV.VUS || 10);
const DURATION = __ENV.DURATION || '2m';

export const options = {
    scenarios: {
        reads: {
            executor: 'constant-vus',
            vus: VUS,
            duration: DURATION,
        },
    },
    // 엔드포인트별 지연시간이 name 태그로 분리되어 집계됨
    thresholds: {
        'http_req_duration{name:GET /travel}': ['p(95)<500'],
        'http_req_duration{name:GET /travel/userdetail}': ['p(95)<500'],
        'http_req_duration{name:GET /payment (all)}': ['p(95)<500'],
        'http_req_duration{name:GET /payment (by date)}': ['p(95)<500'],
        'http_req_duration{name:GET /payment/detail}': ['p(95)<500'],
        'http_req_duration{name:GET /record (by date)}': ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

// setup: 유저 4명 로그인해서 토큰 확보 + travelId/paymentId 목록 확보
export function setup() {
    const tokens = [];
    for (let i = 1; i <= 4; i++) {
        const res = http.post(`${BASE_URL}/user/login`, JSON.stringify({
            email: `loadtest${i}@yoen.test`,
            password: 'LoadTest123!',
        }), { headers: { 'Content-Type': 'application/json' } });
        if (res.status !== 200) {
            throw new Error(`login failed for loadtest${i}: ${res.status} ${res.body}`);
        }
        tokens.push(res.json('data.accessToken'));
    }

    const auth = (t) => ({ headers: { Authorization: `Bearer ${t}` } });

    // 여행 ID 찾기 (LoadTest Trip)
    const travels = http.get(`${BASE_URL}/travel`, auth(tokens[0]));
    const travelList = travels.json('data') || [];
    const trip = travelList.find((t) => t.travelName === 'LoadTest Trip');
    if (!trip) throw new Error('LoadTest Trip not found — run seed.sql first');
    const travelId = trip.travelId;

    // 결제 ID 목록 (detail 조회용) — type 없이 호출하면 빈 결과가 오므로 type 필수
    const payments = http.get(`${BASE_URL}/payment?travelId=${travelId}&type=PAYMENT`, auth(tokens[0]));
    const paymentIds = (payments.json('data') || []).map((p) => p.paymentId);
    if (paymentIds.length === 0) throw new Error('No payments found — run seed.sql first');

    return { tokens, travelId, paymentIds };
}

export default function (data) {
    const token = data.tokens[__VU % data.tokens.length];
    const params = (name) => ({
        headers: { Authorization: `Bearer ${token}` },
        tags: { name },
    });
    const day = (__ITER % 10) + 1;
    const date = `2025-07-${String(day).padStart(2, '0')}T00:00:00`;
    const paymentId = data.paymentIds[__ITER % data.paymentIds.length];

    group('travel', () => {
        const r1 = http.get(`${BASE_URL}/travel`, params('GET /travel'));
        check(r1, { 'travel list 200': (r) => r.status === 200 });

        const r2 = http.get(`${BASE_URL}/travel/userdetail?travelId=${data.travelId}`, params('GET /travel/userdetail'));
        check(r2, { 'travel userdetail 200': (r) => r.status === 200 });
    });

    group('payment', () => {
        const r3 = http.get(`${BASE_URL}/payment?travelId=${data.travelId}&type=PAYMENT`, params('GET /payment (all)'));
        check(r3, { 'payment all 200': (r) => r.status === 200 });

        const r4 = http.get(`${BASE_URL}/payment?travelId=${data.travelId}&date=${date}&type=PAYMENT`, params('GET /payment (by date)'));
        check(r4, { 'payment by date 200': (r) => r.status === 200 });

        const r5 = http.get(`${BASE_URL}/payment/detail?paymentId=${paymentId}`, params('GET /payment/detail'));
        check(r5, { 'payment detail 200': (r) => r.status === 200 });
    });

    group('record', () => {
        const r6 = http.get(`${BASE_URL}/record?travelId=${data.travelId}&date=${date}`, params('GET /record (by date)'));
        check(r6, { 'record by date 200': (r) => r.status === 200 });
    });
}
