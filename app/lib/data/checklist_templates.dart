// 체크리스트 템플릿: 기본 + 날씨(기온·비) + 레저 활동별
// 여행 설정(날씨·활동)을 바꾸면 템플릿 항목이 다시 맞춰지고, 직접 추가한 항목과 체크 상태는 유지됨
import 'models.dart';

class CheckTemplate {
  final String key;
  final String label;
  final List<(String group, String text)> items;
  const CheckTemplate(this.key, this.label, this.items);
}

const base = CheckTemplate('base', '기본', [
  ('서류', '여권 (유효기간 6개월 이상)'),
  ('서류', '여권 사본·사진 (폰에 저장)'),
  ('서류', '항공권 e-티켓'),
  ('서류', '숙소 예약 확인서'),
  ('서류', '여행자 보험'),
  ('서류', '비자·입국 신고 확인'),
  ('돈', '해외 결제 카드'),
  ('돈', '현지 통화 현금'),
  ('전자기기', '유심·eSIM·로밍'),
  ('전자기기', '충전기·케이블'),
  ('전자기기', '보조배터리 (기내 반입)'),
  ('전자기기', '멀티 어댑터'),
  ('생활', '상비약 (진통제·소화제·밴드)'),
  ('생활', '세면도구'),
  ('생활', '편한 신발'),
]);

const climateTemplates = {
  Climate.hot: CheckTemplate('hot', '더움', [
    ('옷·날씨', '반팔·반바지'),
    ('옷·날씨', '선크림'),
    ('옷·날씨', '선글라스'),
    ('옷·날씨', '모자'),
    ('옷·날씨', '샌들·슬리퍼'),
    ('옷·날씨', '얇은 긴팔 (실내 냉방)'),
    ('생활', '모기 기피제'),
    ('생활', '휴대용 선풍기'),
  ]),
  Climate.mild: CheckTemplate('mild', '선선', [('옷·날씨', '얇은 겉옷·가디건'), ('옷·날씨', '긴바지'), ('옷·날씨', '아침저녁용 니트')]),
  Climate.cold: CheckTemplate('cold', '추움', [
    ('옷·날씨', '패딩·두꺼운 외투'),
    ('옷·날씨', '내복·히트텍'),
    ('옷·날씨', '장갑'),
    ('옷·날씨', '목도리·비니'),
    ('옷·날씨', '핫팩'),
    ('생활', '보습 크림·립밤'),
  ]),
};

const rainTemplate = CheckTemplate('rain', '비', [('옷·날씨', '접이식 우산'), ('옷·날씨', '우비'), ('옷·날씨', '방수 신발'), ('생활', '방수팩·지퍼백')]);

const activityTemplates = [
  CheckTemplate('golf', '골프', [
    ('골프', '골프화'),
    ('골프', '골프 장갑'),
    ('골프', '공·티·마커'),
    ('골프', '골프웨어·모자'),
    ('골프', '티타임 예약 확인'),
    ('골프', '캐디피·팁 현금'),
    ('골프', '골프백 항공 위탁 확인'),
  ]),
  CheckTemplate('freediving', '프리다이빙', [
    ('프리다이빙', '마스크·스노클'),
    ('프리다이빙', '롱핀'),
    ('프리다이빙', '웨트슈트'),
    ('프리다이빙', '웨이트 벨트·납'),
    ('프리다이빙', '자격증 카드'),
    ('프리다이빙', '다이빙 컴퓨터'),
    ('프리다이빙', '귀 세척액·코 스프레이'),
    ('프리다이빙', '래시가드'),
  ]),
  CheckTemplate('scuba', '스쿠버', [
    ('스쿠버', '자격증 카드·로그북'),
    ('스쿠버', '마스크'),
    ('스쿠버', '다이빙 컴퓨터'),
    ('스쿠버', '래시가드·웨트슈트'),
    ('스쿠버', '마지막 다이빙 후 비행 18시간 확인'),
  ]),
  CheckTemplate('snorkel', '스노클링', [('스노클링', '스노클 마스크'), ('스노클링', '아쿠아슈즈'), ('스노클링', '래시가드'), ('스노클링', '산호 보호 선크림')]),
  CheckTemplate('beach', '해변·수영', [('해변', '수영복'), ('해변', '비치 타월'), ('해변', '방수 폰 케이스'), ('해변', '아쿠아슈즈')]),
  CheckTemplate('surf', '서핑', [('서핑', '래시가드'), ('서핑', '서프 왁스·리쉬'), ('서핑', '강습 예약 확인')]),
  CheckTemplate('ski', '스키·보드', [('스키', '스키복·고글'), ('스키', '넥워머·장갑'), ('스키', '리프트권 예약'), ('스키', '장비 렌탈 예약')]),
  CheckTemplate('hiking', '등산·트레킹', [
    ('트레킹', '등산화'),
    ('트레킹', '바람막이'),
    ('트레킹', '배낭·물병'),
    ('트레킹', '헤드랜턴'),
    ('트레킹', '트레킹 허가·예약 확인'),
  ]),
  CheckTemplate('camping', '캠핑', [('캠핑', '침낭'), ('캠핑', '랜턴'), ('캠핑', '캠핑장 예약 확인')]),
  CheckTemplate('business', '출장', [('출장', '정장·구두'), ('출장', '명함'), ('출장', '노트북·충전기')]),
  CheckTemplate('kids', '아이 동반', [('아이', '아이 여권'), ('아이', '기저귀·물티슈'), ('아이', '아이 상비약'), ('아이', '간식·장난감')]),
];

/// 첫 도시 위도와 출발 월로 날씨 추정 (언제든 직접 바꿀 수 있음)
Climate guessClimate(double? lat, DateTime start) {
  if (lat == null) return Climate.mild;
  if (lat.abs() < 23.5) return Climate.hot;
  final m = start.month;
  final north = lat >= 0;
  final summer = north ? (m >= 6 && m <= 8) : (m == 12 || m <= 2);
  final winter = north ? (m == 12 || m <= 2) : (m >= 6 && m <= 8);
  if (summer) return lat.abs() > 55 ? Climate.mild : Climate.hot;
  if (winter) return lat.abs() < 30 ? Climate.mild : Climate.cold;
  if (lat.abs() > 50 && (north ? (m == 3 || m == 11) : (m == 5 || m == 9))) return Climate.cold;
  return Climate.mild;
}

String _key(String src, String text) => '$src|$text';

/// 여행 설정에 맞게 템플릿 항목을 다시 맞춤
void syncChecklist(Trip t) {
  final want = <CheckTemplate>[
    base,
    climateTemplates[t.climate]!,
    if (t.rainy) rainTemplate,
    ...activityTemplates.where((a) => t.activities.contains(a.key)),
  ];
  final old = {for (final c in t.checklist) _key(c.src, c.text): c};
  final next = <CheckItem>[];
  for (final tpl in want) {
    for (final (group, text) in tpl.items) {
      final k = _key(tpl.key, text);
      if (t.hiddenChecks.contains(k)) continue;
      next.add(old[k] ?? CheckItem(text: text, group: group, src: tpl.key));
    }
  }
  next.addAll(t.checklist.where((c) => c.src == 'custom'));
  t.checklist = next;
}

/// 템플릿 항목을 지우면 다음에 다시 생기지 않게 기록
void hideCheck(Trip t, CheckItem c) {
  if (c.src != 'custom') t.hiddenChecks.add(_key(c.src, c.text));
  t.checklist.remove(c);
}

const groupOrder = ['서류', '돈', '전자기기', '옷·날씨', '생활'];
