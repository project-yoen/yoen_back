-- YOEN 부하테스트 시드 데이터
-- 사전 조건: loadtest/seed_users.js 로 유저 4명이 먼저 등록되어 있어야 함 (bcrypt 해시 때문에 API로 등록)
-- 실행: docker exec -i <postgres-container> psql -U <user> -d <db> < loadtest/seed.sql
-- 재실행해도 중복 생성되지 않도록 작성됨 (LoadTest Trip 존재 여부 기준)

BEGIN;

-- 카테고리 (없으면 생성)
INSERT INTO categories (category_name, type, created_at, updated_at, is_active)
SELECT v.name, v.type, now(), now(), true
FROM (VALUES ('식비', 'PAYMENT'), ('교통', 'PAYMENT'), ('숙박', 'PAYMENT'), ('쇼핑', 'PAYMENT')) AS v(name, type)
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.category_name = v.name);

-- 환율 (결제 생성 API가 환율 데이터를 요구함)
INSERT INTO exchangerates (exchange_rate, created_at, updated_at, is_active)
SELECT 9.5, now() - interval '30 days', now(), true
WHERE NOT EXISTS (SELECT 1 FROM exchangerates);

-- 여행 (이미 있으면 전체 시드 스킵)
DO $$
DECLARE
  v_travel_id bigint;
BEGIN
  IF EXISTS (SELECT 1 FROM travels WHERE travel_name = 'LoadTest Trip') THEN
    RAISE NOTICE 'LoadTest Trip already seeded, skipping';
    RETURN;
  END IF;

  INSERT INTO travels (travel_name, num_of_people, num_of_joined_people, nation, start_date, end_date, shared_fund, created_at, updated_at, is_active)
  VALUES ('LoadTest Trip', 4, 4, 'JAPAN', DATE '2025-07-01', DATE '2025-07-10', 1000000, now(), now(), true)
  RETURNING travel_id INTO v_travel_id;

  -- 여행 유저 4명 (loadtest 유저 이메일 기준, 전원 WRITER)
  INSERT INTO travelusers (travel_id, user_id, role, travel_nickname, created_at, updated_at, is_active)
  SELECT v_travel_id, u.user_id, 'WRITER', u.nickname, now(), now(), true
  FROM users u
  WHERE u.email IN ('loadtest1@yoen.test', 'loadtest2@yoen.test', 'loadtest3@yoen.test', 'loadtest4@yoen.test');

  -- 결제 200건 (10일에 걸쳐 분산, 카테고리/결제자 로테이션)
  INSERT INTO payments (travel_id, category_id, traveluser_id, payment_name, payment_method, payer_type, pay_time, payment_account, currency, type, exchange_rate, created_at, updated_at, is_active)
  SELECT
    v_travel_id,
    (SELECT category_id FROM categories ORDER BY category_id OFFSET (g % 4) LIMIT 1),
    (SELECT travel_user_id FROM travelusers WHERE travel_id = v_travel_id ORDER BY travel_user_id OFFSET (g % 4) LIMIT 1),
    'seed payment ' || g,
    'CARD',
    'INDIVIDUAL',
    TIMESTAMP '2025-07-01 09:00:00' + ((g % 10) || ' days')::interval + ((g / 10) || ' minutes')::interval,
    1000 + (g * 37) % 9000,
    'YEN',
    'PAYMENT',
    9.5,
    now(), now(), true
  FROM generate_series(1, 200) AS g;

  -- 정산 1건/결제 + 정산유저 4명/정산 (getDetailPayment 다층 N+1 노출용)
  INSERT INTO settlements (payment_id, settlement_name, amount, is_paid, created_at, updated_at, is_active)
  SELECT p.payment_id, 'seed settlement', p.payment_account, false, now(), now(), true
  FROM payments p
  WHERE p.travel_id = v_travel_id AND p.payment_name LIKE 'seed payment %';

  INSERT INTO settlementusers (settlement_id, traveluser_id, amount, is_paid, created_at, updated_at, is_active)
  SELECT s.settlement_id, tu.traveluser_id, s.amount / 4, false, now(), now(), true
  FROM settlements s
  JOIN payments p ON p.payment_id = s.payment_id AND p.travel_id = v_travel_id
  CROSS JOIN (SELECT travel_user_id AS traveluser_id FROM travelusers WHERE travel_id = v_travel_id) tu
  WHERE s.settlement_name = 'seed settlement';

  -- 여행기록 50건
  INSERT INTO travelrecords (travel_id, traveluser_id, title, content, record_time, created_at, updated_at, is_active)
  SELECT
    v_travel_id,
    (SELECT travel_user_id FROM travelusers WHERE travel_id = v_travel_id ORDER BY travel_user_id OFFSET (g % 4) LIMIT 1),
    'seed record ' || g,
    '부하테스트용 여행기록 본문 ' || g,
    TIMESTAMP '2025-07-01 12:00:00' + ((g % 10) || ' days')::interval + ((g / 10) || ' hours')::interval,
    now(), now(), true
  FROM generate_series(1, 50) AS g;

  -- 기록 이미지 (기록당 2장, 더미 URL — 이미지 lazy 로딩 N+1 노출용)
  INSERT INTO images (user_id, object_key, image_url, created_at, updated_at, is_active)
  SELECT
    (SELECT u.user_id FROM users u WHERE u.email = 'loadtest1@yoen.test'),
    'loadtest/dummy_' || tr.travel_record_id || '_' || n,
    'https://example.com/loadtest/dummy_' || tr.travel_record_id || '_' || n || '.jpg',
    now(), now(), true
  FROM travelrecords tr
  CROSS JOIN generate_series(1, 2) AS n
  WHERE tr.travel_id = v_travel_id;

  INSERT INTO travelrecordimages (travelrecord_id, image_id, created_at, updated_at, is_active)
  SELECT tr.travel_record_id, i.image_id, now(), now(), true
  FROM travelrecords tr
  JOIN images i ON i.object_key LIKE 'loadtest/dummy_' || tr.travel_record_id || '\_%'
  WHERE tr.travel_id = v_travel_id;

  RAISE NOTICE 'Seed complete: travel_id=%', v_travel_id;
END $$;

COMMIT;
