import http from 'k6/http';
import { sleep, check } from 'k6';

export const options = {
    vus: 10,           // usuarios virtuales
    duration: '2m',    // duración total
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'], // p95 < 500ms (ms); solo referencia. Observa el panel en Grafana
    },
};

const BASE = __ENV.BASE_URL || 'http://backend:8080';

export default function () {
    const urls = [
        `${BASE}/actuator/health`,
        `${BASE}/api-docs`,
        `${BASE}/swagger`,
    ];
    const url = urls[Math.floor(Math.random() * urls.length)];
    const res = http.get(url);
    check(res, {
        'status < 500': (r) => r.status < 500,
    });
    sleep(0.5);
}
