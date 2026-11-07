// 새 여행 만들기 / 여행 정보 고치기
import 'package:flutter/material.dart';

import '../data/catalog.dart';
import '../data/models.dart';
import '../data/store.dart';
import '../ui/route.dart';
import '../ui/sheet.dart';
import '../ui/theme.dart';
import '../util/format.dart';
import 'trip_screen.dart';

class TripEditScreen extends StatefulWidget {
  final String? tripId;
  const TripEditScreen({super.key, this.tripId});
  @override
  State<TripEditScreen> createState() => _TripEditScreenState();
}

class _TripEditScreenState extends State<TripEditScreen> {
  final _name = TextEditingController();
  final _budget = TextEditingController();
  final _q = TextEditingController();
  late DateTime _start;
  List<Leg> _legs = [];
  bool _nameTouched = false;
  bool _dirty = false;
  bool _loaded = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_loaded) return;
    _loaded = true;
    final t = widget.tripId == null ? null : StoreScope.of(context).byId(widget.tripId!);
    if (t != null) {
      _name.text = t.title;
      _nameTouched = true;
      _start = t.start;
      _legs = t.legs.map((l) => Leg(destId: l.destId, cityKo: l.cityKo, code: l.code, nights: l.nights)).toList();
      if (t.budget > 0) _budget.text = comma(t.budget);
    } else {
      final n = DateTime.now().add(const Duration(days: 30));
      _start = DateTime(n.year, n.month, n.day);
    }
  }

  @override
  void dispose() {
    _name.dispose();
    _budget.dispose();
    _q.dispose();
    super.dispose();
  }

  void _autoName() {
    if (_nameTouched) return;
    _name.text = _legs.map((l) => l.cityKo).toSet().join('·');
  }

  void _addCity(Destination d) {
    _dirty = true;
    setState(() {
      _legs.add(Leg(destId: d.id, cityKo: d.ko, code: d.code, nights: 2));
      _q.clear();
      _autoName();
    });
    FocusScope.of(context).unfocus();
  }

  Future<void> _pickDate() async {
    final d = await showDatePicker(
      context: context,
      initialDate: _start,
      firstDate: DateTime(2020),
      lastDate: DateTime(2040),
      helpText: '출발일',
    );
    if (d != null) setState(() => _start = d);
    _dirty = true;
  }

  void _save() {
    if (_legs.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('도시를 하나 이상 넣어줘')));
      return;
    }
    final store = StoreScope.of(context);
    final title = _name.text.trim().isEmpty ? _legs.map((l) => l.cityKo).join('·') : _name.text.trim();
    final budget = parseAmount(_budget.text)?.round() ?? 0;
    _dirty = false;
    final existing = widget.tripId == null ? null : store.byId(widget.tripId!);
    if (existing != null) {
      store.updateTripBasics(existing, title: title, start: _start, legs: _legs, budget: budget);
      Navigator.pop(context);
    } else {
      final t = store.createTrip(title: title, start: _start, legs: _legs, budget: budget);
      Navigator.pushReplacement(context, slideRoute(TripScreen(tripId: t.id)));
    }
  }

  Future<void> _delete() async {
    final store = StoreScope.of(context);
    final t = store.byId(widget.tripId!);
    if (t == null) return;
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: const Text('여행 삭제'),
        content: Text('"${t.title}" 일정·체크리스트·지출이 모두 지워짐.', style: sans(14, c: C.muted)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('취소')),
          TextButton(
            onPressed: () => Navigator.pop(c, true),
            child: Text(
              '삭제',
              style: sans(14, c: C.red, w: FontWeight.w800),
            ),
          ),
        ],
      ),
    );
    if (ok == true && mounted) {
      store.deleteTrip(t);
      Navigator.of(context).popUntil((r) => r.isFirst);
    }
  }

  @override
  Widget build(BuildContext context) {
    final cat = StoreScope.of(context).catalog!;
    final results = cat.searchCities(_q.text);
    final nights = _legs.fold(0, (a, l) => a + l.nights);
    final end = _start.add(Duration(days: nights));
    var cursor = _start;

    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) async {
        if (didPop) return;
        if (!_dirty || await confirmDiscard(context)) {
          _dirty = false;
          if (context.mounted) Navigator.pop(context);
        }
      },
      child: Scaffold(
        appBar: AppBar(
          leading: IconButton(icon: const Icon(Icons.close_rounded), onPressed: () => Navigator.maybePop(context)),
          title: Text(widget.tripId == null ? '새 여행' : '여행 정보', style: sans(18, w: FontWeight.w800)),
          actions: [
            if (widget.tripId != null)
              IconButton(tooltip: '여행 삭제', onPressed: _delete, icon: const Icon(Icons.delete_outline_rounded)),
          ],
        ),
        body: SafeArea(
          top: false,
          child: Column(
            children: [
              Expanded(
                child: ListView(
                  padding: const EdgeInsets.fromLTRB(18, 4, 18, 20),
                  keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
                  children: [
                    const Eyebrow('TRIP NAME'),
                    const SizedBox(height: 8),
                    TextField(
                      controller: _name,
                      onChanged: (_) => _nameTouched = _dirty = true,
                      style: sans(17, w: FontWeight.w800),
                      decoration: const InputDecoration(hintText: '도시를 넣으면 자동으로 채워짐'),
                    ),
                    const SizedBox(height: 18),
                    const Eyebrow('DEPART'),
                    const SizedBox(height: 8),
                    Press(
                      onTap: _pickDate,
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
                            const Icon(Icons.event_rounded, size: 18, color: C.muted),
                            const SizedBox(width: 10),
                            Text('${ymd(_start)} ${wkEn(_start)}', style: mono(15, bold: true)),
                            const Spacer(),
                            Text('바꾸기', style: sans(13, c: C.muted)),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 18),
                    const Eyebrow('ITINERARY'),
                    const SizedBox(height: 8),
                    if (_legs.isEmpty) const EmptyNote('도시를 추가해줘', '여러 도시를 순서대로 넣으면 구간(LEG)으로 이어짐. 예: 오사카 2박 → 교토 2박'),
                    for (var i = 0; i < _legs.length; i++) ...[
                      if (i > 0)
                        const Padding(
                          padding: EdgeInsets.only(left: 30),
                          child: Icon(Icons.arrow_downward_rounded, size: 16, color: C.faint),
                        ),
                      Builder(
                        builder: (_) {
                          final l = _legs[i];
                          final from = cursor;
                          cursor = cursor.add(Duration(days: l.nights));
                          return SlideIn(index: 0, child: _legTile(i, l, from, cursor));
                        },
                      ),
                    ],
                    const SizedBox(height: 18),
                    const Eyebrow('ADD CITY'),
                    const SizedBox(height: 8),
                    TextField(
                      controller: _q,
                      onChanged: (_) => setState(() {}),
                      style: sans(15),
                      decoration: InputDecoration(
                        hintText: '도시·국가·공항 코드 (예: 교토, 베트남, CEB)',
                        prefixIcon: const Icon(Icons.search_rounded, color: C.muted),
                        suffixIcon: _q.text.isEmpty
                            ? null
                            : IconButton(
                                tooltip: '지우기',
                                onPressed: () => setState(_q.clear),
                                icon: const Icon(Icons.cancel_rounded, color: C.faint),
                              ),
                      ),
                    ),
                    if (results.isNotEmpty) ...[
                      const SizedBox(height: 8),
                      Container(
                        decoration: BoxDecoration(
                          color: C.paper,
                          borderRadius: BorderRadius.circular(12),
                          boxShadow: const [BoxShadow(color: Color(0x1A14213D), blurRadius: 14, offset: Offset(0, 6))],
                        ),
                        child: Column(children: [for (final d in results.take(8)) _cityRow(cat, d)]),
                      ),
                    ],
                    const SizedBox(height: 18),
                    const Eyebrow('BUDGET · 원'),
                    const SizedBox(height: 8),
                    TextField(
                      controller: _budget,
                      keyboardType: TextInputType.number,
                      inputFormatters: const [ThousandsFormatter(decimal: false)],
                      onChanged: (_) => _dirty = true,
                      style: mono(16, bold: true),
                      decoration: const InputDecoration(hintText: '예산 (선택) 예: 2,000,000', suffixText: '원'),
                    ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(18, 8, 18, 14),
                child: Column(
                  children: [
                    Row(
                      children: [
                        Text('${md(_start)} ${wkEn(_start)} → ${md(end)} ${wkEn(end)}', style: mono(12, c: C.muted)),
                        const Spacer(),
                        Text('${nights}N ${nights + 1}D', style: mono(13, bold: true)),
                      ],
                    ),
                    const SizedBox(height: 10),
                    PassButton(widget.tripId == null ? '여행 만들기' : '저장', primary: true, height: 56, onTap: _save),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _cityRow(Catalog cat, Destination d) {
    final gw = d.noAirport ? cat.byId[d.gateway] : null;
    final sub = [d.country, if (gw != null) '공항 없음 · 관문 ${gw.ko}', if (d.travelBan) '여행경보 지역'].join(' · ');
    return InkWell(
      onTap: () => _addCity(d),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
        decoration: const BoxDecoration(
          border: Border(bottom: BorderSide(color: Color(0xFFEDF0F4))),
        ),
        child: Row(
          children: [
            SizedBox(width: 48, child: Text(d.code, style: mono(14, bold: true))),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(d.ko, style: sans(15, w: FontWeight.w800)),
                  Text(sub, style: sans(12, c: d.travelBan ? C.red : C.muted)),
                ],
              ),
            ),
            Text('${cat.places[d.id]?.length ?? 0}곳', style: mono(11, c: C.faint)),
          ],
        ),
      ),
    );
  }

  Widget _legTile(int i, Leg l, DateTime from, DateTime to) {
    return Ticket(
      stub: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(l.code, style: mono(20, bold: true)),
          Text('LEG ${i + 1}', style: mono(10, c: C.faint)),
        ],
      ),
      body: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(l.cityKo, style: sans(16, w: FontWeight.w800)),
                Text('${md(from)} – ${md(to)}', style: mono(11.5, c: C.muted)),
              ],
            ),
          ),
          Container(
            decoration: BoxDecoration(
              border: Border.all(color: C.line, width: 1.5),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                _step(Icons.remove_rounded, '1박 줄이기', () {
                  _dirty = true;
                  if (l.nights > 0) setState(() => l.nights--);
                }),
                SizedBox(
                  width: 34,
                  child: Text('${l.nights}박', textAlign: TextAlign.center, style: mono(13, bold: true)),
                ),
                _step(
                  Icons.add_rounded,
                  '1박 늘리기',
                  () => setState(() {
                    _dirty = true;
                    l.nights++;
                  }),
                ),
              ],
            ),
          ),
        ],
      ),
      trailing: IconButton(
        tooltip: '구간 빼기',
        onPressed: () => setState(() {
          _legs.removeAt(i);
          _autoName();
        }),
        icon: const Icon(Icons.close_rounded, size: 18, color: C.faint),
      ),
    );
  }

  Widget _step(IconData icon, String tip, VoidCallback onTap) => SizedBox(
    width: 34,
    height: 40,
    child: IconButton(
      padding: EdgeInsets.zero,
      tooltip: tip,
      onPressed: onTap,
      icon: Icon(icon, size: 18, color: C.ink),
    ),
  );
}
