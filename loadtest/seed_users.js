// 부하테스트용 유저 4명 등록 (bcrypt 해시 때문에 SQL이 아닌 API로 등록)
// 실행: k6 run loadtest/seed_users.js
import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = { vus: 1, iterations: 1 };

export default function () {
    for (let i = 1; i <= 4; i++) {
        const email = `loadtest${i}@yoen.test`;

        const exists = http.get(`${BASE_URL}/user/exists?email=${email}`);
        if (exists.status === 200 && exists.json('data') === true) {
            console.log(`${email} already exists, skipping`);
            continue;
        }

        const res = http.post(`${BASE_URL}/user/register`, JSON.stringify({
            password: 'LoadTest123!',
            name: `로드테스트${i}`,
            email: email,
            nickname: `loadtest${i}`,
            gender: 'OTHERS',
            birthday: '1995-01-01',
            profileImageUrl: null,
        }), { headers: { 'Content-Type': 'application/json' } });

        check(res, { [`register ${email}`]: (r) => r.status === 200 });
        console.log(`${email}: ${res.status}`);
    }
}
