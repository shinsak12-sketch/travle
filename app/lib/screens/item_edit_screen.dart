// 일정 카드 입력: 항공편 · 숙소 · 맛집 · 쇼핑 · 장소 · 메모 (모두 직접 입력)
import 'package:flutter/material.dart';

import '../data/models.dart';
import '../data/store.dart';
import '../ui/item_card.dart';
import '../ui/sheet.dart';
import '../ui/theme.dart';
import '../util/format.dart';
import '../util/maps.dart';

const currencies = [
  'KRW',
  'JPY',
  'USD',
  'EUR',
  'CNY',
  'TWD',
  'HKD',
  'VND',
  'THB',
  'PHP',
  'SGD',
  'MYR',
  'IDR',
  'AUD',
  'NZD',
  'GBP',
  'CHF',
  'CAD',
];

class _F {
  final String key, label, hint;
  final bool mono, multi, upper;
  const _F(this.key, this.label, {this.hint = '', this.mono = false, this.multi = false, this.upper = false});
}

/// 종류별 입력 칸 (title·time은 공통이지만 이름을 다르게 보여줌)
const _titleLabel = {
  ItemType.flight: '편명 (예: KE723)',
  ItemType.stay: '숙소 이름',
  ItemType.food: '가게 이름',
  ItemType.shopping: '가게·장소 이름',
  ItemType.place: '장소 이름',
  ItemType.memo: '제목',
};
const _timeLabel = {
  ItemType.flight: '출발 시각',
  ItemType.stay: '체크인',
  ItemType.food: '시간',
  ItemType.shopping: '시간',
  ItemType.place: '시간',
  ItemType.memo: '시간',
};
const _fields = {
  ItemType.flight: [
    _F('from', '출발 공항', hint: 'ICN', mono: true, upper: true),
    _F('to', '도착 공항', hint: 'KIX', mono: true, upper: true),
    _F('arrive', '도착 시각 (현지)', hint: '12:15', mono: true),
    _F('terminal', '터미널', hint: 'T1', mono: true),
    _F('seat', '좌석', hint: '32A', mono: true),
    _F('booking', '예약번호', hint: 'ABC123', mono: true, upper: true),
  ],
  ItemType.stay: [
    _F('checkout', '체크아웃 (시각·날짜)', hint: '11/22 11:00', mono: true),
    _F('nights', '박 수', hint: '2', mono: true),
    _F('address', '주소', hint: '구글지도 검색에 쓰임'),
    _F('booking', '예약번호', mono: true),
    _F('maps', '구글지도 링크 (선택)', hint: '공유로 복사한 링크'),
  ],
  ItemType.food: [
    _F('menu', '먹을 메뉴', hint: '돈코츠 라멘, 교자'),
    _F('hours', '영업시간·휴무', hint: '10:00–20:00 · 화 휴무'),
    _F('reservation', '예약 정보', hint: '19:00 2명 · 예약자 이름'),
    _F('address', '주소', hint: '구글지도 검색에 쓰임'),
    _F('maps', '구글지도 링크 (선택)', hint: '공유로 복사한 링크'),
  ],
  ItemType.shopping: [
    _F('buy', '살 것', hint: '동전파스, 곤약젤리, 선물용 과자', multi: true),
    _F('hours', '영업시간·휴무', hint: '10:00–20:00 · 화 휴무'),
    _F('address', '주소', hint: '구글지도 검색에 쓰임'),
    _F('maps', '구글지도 링크 (선택)', hint: '공유로 복사한 링크'),
  ],
  ItemType.place: [
    _F('hours', '영업시간·휴무', hint: '10:00–20:00 · 화 휴무'),
    _F('address', '주소', hint: '구글지도 검색에 쓰임'),
    _F('maps', '구글지도 링크 (선택)', hint: '공유로 복사한 링크'),
  ],
  ItemType.memo: <_F>[],
};

class ItemEditScreen extends StatefulWidget {
  final String tripId;
  final String? itemId;
  final ItemType? newType;
  final int? day;
  const ItemEditScreen({super.key, required this.tripId, this.itemId, this.newType, this.day});
  @override
  State<ItemEditScreen> createState() => _ItemEditScreenState();
}

class _ItemEditScreenState extends State<ItemEditScreen> {
  late TripItem _item; // 편집용 사본
  TripItem? _orig;
  int? _day;
  late ItemType _type;
  final _c = <String, TextEditingController>{};
  bool _init = false;
  bool _saved = false;

  TextEditingController _ctl(String k, String v) => _c.putIfAbsent(k, () => TextEditingController(text: v));

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_init) return;
    _init = true;
    final store = StoreScope.of(context);
    final t = store.byId(widget.tripId)!;
    if (widget.itemId != null) {
      _orig = [...t.wish, ...t.days.expand((d) => d)].firstWhere((i) => i.id == widget.itemId);
      _item = _orig!.copy();
      _day = store.findDay(t, _orig!);
    } else {
      _item = TripItem(type: widget.newType ?? ItemType.memo, title: '');
      _day = widget.day;
      if (_item.type == ItemType.flight && t.days.isNotEmpty) _item.f['from'] = _day == 0 ? 'ICN' : '';
    }
    _type = _item.type;
    _item.currency = _item.cost == null ? _defaultCurrency(t) : _item.currency;
  }

  String _defaultCurrency(Trip t) => _item.type == ItemType.flight ? 'KRW' : (t.rates.keys.firstOrNull ?? 'KRW');

  @override
  void dispose() {
    for (final c in _c.values) {
      c.dispose();
    }
    super.dispose();
  }

  bool get _fromCatalog => _item.type == ItemType.place && _item.destId != null;

  Future<void> _pickTime() async {
    final parts = _item.time.split(':');
    final init = parts.length == 2
        ? TimeOfDay(hour: int.tryParse(parts[0]) ?? 9, minute: int.tryParse(parts[1]) ?? 0)
        : const TimeOfDay(hour: 10, minute: 0);
    final r = await showTimePicker(context: context, initialTime: init, helpText: _timeLabel[_type]);
    if (r != null) setState(() => _item.time = hhmm(r.hour, r.minute));
  }

  void _save() {
    final store = StoreScope.of(context);
    final t = store.byId(widget.tripId)!;
    final title = _ctl('title', _item.title).text.trim();
    if (title.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('${_titleLabel[_type]}을 적어줘')));
      return;
    }
    _item.title = title;
    _item.memo = _ctl('memo', _item.memo).text.trim();
    for (final f in _fields[_type]!) {
      var v = _ctl(f.key, _item.get(f.key)).text.trim();
      if (f.upper) v = v.toUpperCase();
      if (v.isEmpty) {
        _item.f.remove(f.key);
      } else {
        _item.f[f.key] = v;
      }
    }
    _item.cost = parseAmount(_ctl('cost', amountText(_item.cost)).text);
    _saved = true;
    if (_item.cost != null && _item.currency != 'KRW' && !t.rates.containsKey(_item.currency)) {
      t.rates[_item.currency] = 0; // 예산 탭에서 환율 넣도록 표시
    }

    final orig = _orig;
    if (orig == null) {
      store.addItem(t, _item, day: _day);
    } else {
      final j = _item.toJson();
      final fresh = TripItem.fromJson(j);
      orig
        ..title = fresh.title
        ..time = fresh.time
        ..memo = fresh.memo
        ..cost = fresh.cost
        ..currency = fresh.currency
        ..f = fresh.f
        ..done = fresh.done;
      final was = store.findDay(t, orig);
      if (was != _day) {
        store.moveItem(t, orig, _day);
      } else {
        store.updateItem(t, orig);
      }
    }
    Navigator.pop(context);
  }

  Future<void> _delete() async {
    final store = StoreScope.of(context);
    final messenger = ScaffoldMessenger.of(context);
    final t = store.byId(widget.tripId)!;
    final item = _orig!;
    final day = store.findDay(t, item);
    final at = day == null ? t.wish.indexOf(item) : t.days[day].indexOf(item);
    final linked = t.expenses.where((e) => e.itemId == item.id).toList();
    store.removeItem(t, item);
    _saved = true;
    Navigator.pop(context);
    messenger
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text('"${item.title}" 지움'),
          action: SnackBarAction(
            label: '되돌리기',
            textColor: const Color(0xFFFF8A8C),
            onPressed: () {
              final list = day == null ? t.wish : t.days[day.clamp(0, t.days.length - 1)];
              list.insert(at.clamp(0, list.length), item);
              t.expenses.addAll(linked);
              store.changed();
            },
          ),
        ),
      );
  }

  /// 입력한 게 있으면 닫을 때 한 번 물어봄
  bool get _dirty {
    if (_saved) return false;
    final o = _orig;
    String text(String k, String v) => _c[k]?.text.trim() ?? v;
    if (o == null) return _c.values.any((c) => c.text.trim().isNotEmpty) || _item.time.isNotEmpty;
    return text('title', o.title) != o.title ||
        text('memo', o.memo) != o.memo ||
        _item.time != o.time ||
        _item.done != o.done ||
        _item.currency != o.currency ||
        parseAmount(text('cost', amountText(o.cost))) != o.cost ||
        _fields[_type]!.any((f) => text(f.key, o.get(f.key)).toUpperCase() != o.get(f.key).toUpperCase());
  }

  @override
  Widget build(BuildContext context) {
    final store = StoreScope.of(context);
    final t = store.byId(widget.tripId)!;
    final cat = store.catalog!;
    final city = _item.destId != null
        ? cat.byId[_item.destId!]
        : (t.legOf(_day ?? 0) == null ? null : cat.byId[t.legOf(_day ?? 0)!.destId]);

    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) async {
        if (didPop) return;
        if (!_dirty || await confirmDiscard(context)) {
          _saved = true;
          if (context.mounted) Navigator.pop(context);
        }
      },
      child: Scaffold(
        appBar: AppBar(
          leading: IconButton(icon: const Icon(Icons.close_rounded), onPressed: () => Navigator.maybePop(context)),
          title: Row(
            children: [
              Icon(itemIcon(_type), size: 20),
              const SizedBox(width: 8),
              Text(_orig == null ? '${_type.label} 추가' : _type.label, style: sans(18, w: FontWeight.w800)),
            ],
          ),
          actions: [
            if (_orig != null) IconButton(tooltip: '삭제', onPressed: _delete, icon: const Icon(Icons.delete_outline_rounded)),
          ],
        ),
        body: SafeArea(
          top: false,
          child: Column(
            children: [
              Expanded(
                child: ListView(
                  padding: const EdgeInsets.fromLTRB(18, 4, 18, 24),
                  keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
                  children: [
                    if (_orig == null && _type != ItemType.flight) ...[
                      Wrap(
                        spacing: 6,
                        runSpacing: 6,
                        children: [
                          for (final ty in [ItemType.food, ItemType.shopping, ItemType.stay, ItemType.place, ItemType.memo])
                            PassChip(
                              ty == ItemType.place ? '장소' : ty.label,
                              selected: _type == ty,
                              onTap: () => setState(() => _type = _item.type = ty),
                            ),
                        ],
                      ),
                      const SizedBox(height: 16),
                    ],
                    _field(
                      _titleLabel[_type]!,
                      _ctl('title', _item.title),
                      fixed: _type == ItemType.flight,
                      enabled: !_fromCatalog,
                      big: true,
                    ),
                    if (_fromCatalog && _item.get('desc').isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: Text(
                          _item.get('desc'),
                          style: sans(14, c: C.muted, h: 1.55, w: FontWeight.w400),
                        ),
                      ),
                    const SizedBox(height: 14),
                    _timeRow(),
                    for (final f in _fields[_type]!)
                      if (!(_fromCatalog && (f.key == 'address' || f.key == 'maps')))
                        Padding(
                          padding: const EdgeInsets.only(top: 14),
                          child: _field(f.label, _ctl(f.key, _item.get(f.key)), hint: f.hint, fixed: f.mono, multi: f.multi),
                        ),
                    const SizedBox(height: 14),
                    _field('메모', _ctl('memo', _item.memo), multi: true, hint: '자유롭게'),
                    if (_type != ItemType.memo) ...[const SizedBox(height: 14), _costRow(t)],
                    const SizedBox(height: 20),
                    const Eyebrow('어느 날?'),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 6,
                      runSpacing: 6,
                      children: [
                        PassChip('담은 곳', selected: _day == null, onTap: () => setState(() => _day = null)),
                        for (var d = 0; d < t.dayCount; d++)
                          PassChip('D${d + 1} ${md(t.dateOf(d))}', selected: _day == d, onTap: () => setState(() => _day = d)),
                      ],
                    ),
                    if (_orig != null) ...[
                      const SizedBox(height: 20),
                      Row(
                        children: [
                          if (_type != ItemType.memo && _type != ItemType.flight)
                            Expanded(
                              child: PassButton(
                                '구글지도',
                                icon: Icons.near_me_outlined,
                                onTap: () => openMaps(_orig!, cityEn: city?.en ?? ''),
                              ),
                            ),
                          if (_type != ItemType.memo && _type != ItemType.flight) const SizedBox(width: 10),
                          Expanded(
                            child: PassButton(
                              _item.done ? '다녀옴 취소' : '다녀옴',
                              icon: Icons.check_rounded,
                              onTap: () => setState(() => _item.done = !_item.done),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(18, 6, 18, 14),
                child: PassButton('저장', primary: true, height: 56, onTap: _save),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _timeRow() => Row(
    children: [
      Expanded(
        child: Press(
          onTap: _pickTime,
          child: Container(
            height: 52,
            padding: const EdgeInsets.symmetric(horizontal: 14),
            decoration: BoxDecoration(
              color: C.paper,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: C.line2, width: 1.5),
            ),
            child: Row(
              children: [
                const Icon(Icons.schedule_rounded, size: 18, color: C.muted),
                const SizedBox(width: 10),
                Text(_timeLabel[_type]!, style: sans(14, c: C.muted)),
                const Spacer(),
                Text(_item.time.isEmpty ? '--:--' : _item.time, style: mono(16, bold: true)),
              ],
            ),
          ),
        ),
      ),
      if (_item.time.isNotEmpty)
        IconButton(
          tooltip: '시간 지우기',
          onPressed: () => setState(() => _item.time = ''),
          icon: const Icon(Icons.close_rounded, color: C.faint),
        ),
    ],
  );

  Widget _costRow(Trip t) {
    final list = {...currencies, ...t.rates.keys, _item.currency}.toList();
    return Row(
      children: [
        Expanded(child: _field('금액 (넣으면 예산에 자동 반영)', _ctl('cost', amountText(_item.cost)), fixed: true, number: true)),
        const SizedBox(width: 8),
        Container(
          height: 56,
          padding: const EdgeInsets.symmetric(horizontal: 10),
          decoration: BoxDecoration(
            color: C.paper,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: C.line2, width: 1.5),
          ),
          child: DropdownButtonHideUnderline(
            child: DropdownButton<String>(
              value: _item.currency,
              style: mono(14, bold: true),
              items: [for (final c in list) DropdownMenuItem(value: c, child: Text(c))],
              onChanged: (v) => setState(() => _item.currency = v ?? 'KRW'),
            ),
          ),
        ),
      ],
    );
  }

  Widget _field(
    String label,
    TextEditingController c, {
    String hint = '',
    bool fixed = false,
    bool multi = false,
    bool enabled = true,
    bool big = false,
    bool number = false,
  }) {
    return TextField(
      controller: c,
      enabled: enabled,
      minLines: multi ? 2 : 1,
      maxLines: multi ? 5 : 1,
      keyboardType: number
          ? const TextInputType.numberWithOptions(decimal: true)
          : (multi ? TextInputType.multiline : TextInputType.text),
      inputFormatters: number ? const [ThousandsFormatter()] : null,
      textInputAction: multi ? TextInputAction.newline : TextInputAction.next,
      style: fixed ? mono(big ? 18 : 15, bold: true) : sans(big ? 17 : 15, w: big ? FontWeight.w800 : FontWeight.w600),
      decoration: InputDecoration(labelText: label, hintText: hint.isEmpty ? null : hint),
    );
  }
}
