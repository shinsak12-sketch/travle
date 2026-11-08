// 여행 화면: 일정 · 체크리스트 · 예산 · 기록
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../data/models.dart';
import '../data/store.dart';
import '../ui/item_card.dart';
import '../ui/route.dart';
import '../ui/sheet.dart';
import '../ui/theme.dart';
import '../util/format.dart';
import '../util/clock.dart';
import '../util/maps.dart';
import 'budget_tab.dart';
import 'checklist_tab.dart';
import 'guide_screen.dart';
import 'item_edit_screen.dart';
import 'record_tab.dart';
import 'today_screen.dart';
import 'trip_edit_screen.dart';
import 'wish_sheet.dart';

class TripScreen extends StatefulWidget {
  final String tripId;
  final int initialTab;
  const TripScreen({super.key, required this.tripId, this.initialTab = 0});
  @override
  State<TripScreen> createState() => _TripScreenState();
}

class _TripScreenState extends State<TripScreen> {
  late int _tab = widget.initialTab;
  int _day = 0;
  bool _dayInit = false;
  final _dayScroll = ScrollController();
  TripItem? _dragging;

  @override
  void dispose() {
    _dayScroll.dispose();
    super.dispose();
  }

  /// 고른 날짜 칩이 화면 가운데쯤 오게
  void _revealDay() => WidgetsBinding.instance.addPostFrameCallback((_) {
    if (!_dayScroll.hasClients) return;
    final target = (_day * 64.0 - 120).clamp(0.0, _dayScroll.position.maxScrollExtent);
    _dayScroll.animateTo(target, duration: const Duration(milliseconds: 300), curve: Curves.easeOutCubic);
  });

  @override
  Widget build(BuildContext context) {
    final store = StoreScope.of(context);
    final t = store.byId(widget.tripId);
    if (t == null) return const Scaffold();
    if (!_dayInit) {
      _dayInit = true;
      _day = t.todayIndex() ?? 0;
      _revealDay();
    }
    _day = _day.clamp(0, t.dayCount - 1);

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            _header(context, t),
            Padding(
              padding: const EdgeInsets.fromLTRB(18, 10, 18, 4),
              child: _Tabs(labels: const ['일정', '체크', '예산', '기록'], index: _tab, onTap: (i) => setState(() => _tab = i)),
            ),
            Expanded(
              child: AnimatedSwitcher(
                duration: const Duration(milliseconds: 220),
                child: switch (_tab) {
                  0 => _schedule(context, store, t),
                  1 => ChecklistTab(key: const ValueKey('c'), trip: t),
                  2 => BudgetTab(key: const ValueKey('b'), trip: t),
                  _ => RecordTab(key: const ValueKey('r'), trip: t),
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _header(BuildContext context, Trip t) {
    final first = t.legs.firstOrNull?.code ?? '---';
    final last = t.legs.length > 1 ? t.legs.last.code : null;
    final live = t.todayIndex() != null;
    return Container(
      margin: const EdgeInsets.fromLTRB(18, 10, 18, 0),
      padding: const EdgeInsets.fromLTRB(4, 6, 14, 12),
      decoration: BoxDecoration(
        color: C.ink,
        borderRadius: BorderRadius.circular(20),
        boxShadow: const [BoxShadow(color: Color(0x4D14213D), blurRadius: 22, offset: Offset(0, 10))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              IconButton(
                tooltip: '뒤로',
                onPressed: () => Navigator.pop(context),
                icon: const Icon(Icons.arrow_back_ios_new_rounded, color: Colors.white, size: 18),
              ),
              Expanded(child: Eyebrow('TRIP PASS · ${t.nights}N${t.dayCount}D', color: C.onInk)),
              if (live)
                _pill('오늘', () => Navigator.pushReplacement(context, slideRoute(TodayScreen(tripId: t.id))))
              else
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(color: C.red, borderRadius: BorderRadius.circular(6)),
                  child: Text(
                    daysUntil(t.start) >= 0 ? 'D-${daysUntil(t.start)}' : 'DONE',
                    style: mono(12, bold: true, c: Colors.white),
                  ),
                ),
              IconButton(
                tooltip: '여행 정보 고치기',
                onPressed: () => Navigator.push(context, slideRoute(TripEditScreen(tripId: t.id))),
                icon: const Icon(Icons.edit_outlined, color: Colors.white, size: 18),
              ),
            ],
          ),
          Padding(
            padding: const EdgeInsets.only(left: 14),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(first, style: mono(28, bold: true, c: Colors.white).copyWith(height: 1)),
                if (last != null) ...[
                  const Padding(
                    padding: EdgeInsets.symmetric(horizontal: 8),
                    child: Icon(Icons.arrow_forward_rounded, color: C.onInk, size: 18),
                  ),
                  Text(last, style: mono(28, bold: true, c: Colors.white).copyWith(height: 1)),
                ],
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    t.title,
                    style: sans(15, w: FontWeight.w800, c: Colors.white),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(left: 14, top: 4),
            child: Text('${md(t.start)} ${wkEn(t.start)} – ${md(t.end)} ${wkEn(t.end)}', style: mono(11.5, c: C.onInk)),
          ),
        ],
      ),
    );
  }

  Widget _pill(String label, VoidCallback onTap) => Press(
    onTap: onTap,
    child: Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(color: C.red, borderRadius: BorderRadius.circular(8)),
      child: Text(
        label,
        style: sans(12.5, w: FontWeight.w800, c: Colors.white),
      ),
    ),
  );

  // ---------------- 일정 ----------------
  // 카드를 길게 눌러 끌기: 날짜 칩에 놓으면 그날로, 다른 카드에 놓으면 그 앞으로, "담은 곳"에 놓으면 되돌림
  Widget _schedule(BuildContext context, Store store, Trip t) {
    final items = t.days[_day];
    final leg = t.legOf(_day);
    final cat = store.catalog!;
    final dest = leg == null ? null : cat.byId[leg.destId];
    final cityEn = dest?.en ?? '';
    final dragging = _dragging != null;
    return Column(
      key: const ValueKey('s'),
      children: [
        SizedBox(
          height: 64,
          child: ListView.separated(
            controller: _dayScroll,
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.fromLTRB(18, 8, 18, 4),
            itemCount: t.dayCount,
            separatorBuilder: (_, _) => const SizedBox(width: 6),
            itemBuilder: (_, i) => _dayChip(store, t, i),
          ),
        ),
        AnimatedSize(
          duration: const Duration(milliseconds: 200),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(18, 10, 18, 8),
            child: dragging
                ? Row(
                    children: [
                      const Icon(Icons.open_with_rounded, size: 18, color: C.red),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          '날짜 칩·다른 카드·담은 곳에 놓기',
                          style: sans(13.5, w: FontWeight.w800, c: C.red),
                        ),
                      ),
                    ],
                  )
                : Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Text(leg?.cityKo ?? '', style: sans(22, w: FontWeight.w800)),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(mdWeek(t.dateOf(_day)), style: sans(13, c: C.muted)),
                          ),
                          Text('${items.length}곳', style: mono(12, c: C.muted)),
                        ],
                      ),
                      if (dest != null && dest.tz.isNotEmpty)
                        LocalClock(
                          tzName: dest.tz,
                          style: mono(11.5, c: C.faint),
                        ),
                    ],
                  ),
          ),
        ),
        Expanded(
          child: items.isEmpty
              ? ListView(
                  padding: const EdgeInsets.fromLTRB(18, 4, 18, 18),
                  children: const [
                    EmptyNote(
                      '이날 일정이 비어 있음',
                      '"담은 곳"에서 꺼내 넣거나 "+ 추가"로 장소·항공편·숙소·쇼핑·맛집 카드를 넣어. 카드를 길게 눌러 끌면 순서를 바꾸거나 다른 날로 옮길 수 있음.',
                    ),
                  ],
                )
              : ListView.builder(
                  padding: const EdgeInsets.fromLTRB(18, 4, 18, 18),
                  itemCount: items.length,
                  itemBuilder: (_, i) {
                    final it = items[i];
                    final card = ItemCard(
                      item: it,
                      number: i + 1,
                      cats: cat.cats,
                      onTap: () => Navigator.push(context, slideRoute(ItemEditScreen(tripId: t.id, itemId: it.id, day: _day))),
                      onMaps: it.type == ItemType.memo || it.type == ItemType.flight ? null : () => openMaps(it, cityEn: cityEn),
                    );
                    return DragTarget<TripItem>(
                      key: ValueKey(it.id),
                      onWillAcceptWithDetails: (d) => d.data != it,
                      onAcceptWithDetails: (d) => _drop(store, t, d.data, _day, i),
                      builder: (context, cand, _) {
                        final over = cand.isNotEmpty;
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 10),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              AnimatedContainer(
                                duration: const Duration(milliseconds: 150),
                                height: over ? 6 : 0,
                                margin: EdgeInsets.only(bottom: over ? 8 : 0),
                                decoration: BoxDecoration(color: C.red, borderRadius: BorderRadius.circular(3)),
                              ),
                              LongPressDraggable<TripItem>(
                                data: it,
                                hapticFeedbackOnStart: true,
                                // 끄는 카드가 손가락 아래에 따라와서 위쪽 날짜 칩이 가려지지 않게
                                dragAnchorStrategy: (d, ctx, pos) =>
                                    Offset((ctx.findRenderObject() as RenderBox).globalToLocal(pos).dx, -36),
                                onDragStarted: () => setState(() => _dragging = it),
                                onDragEnd: (_) => setState(() => _dragging = null),
                                onDraggableCanceled: (_, _) => setState(() => _dragging = null),
                                feedback: Material(
                                  color: Colors.transparent,
                                  child: SizedBox(
                                    width: MediaQuery.of(context).size.width - 36,
                                    child: Transform.rotate(angle: -0.025, child: Opacity(opacity: .92, child: card)),
                                  ),
                                ),
                                childWhenDragging: Opacity(opacity: .25, child: card),
                                child: SlideIn(index: i, child: card),
                              ),
                            ],
                          ),
                        );
                      },
                    );
                  },
                ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(18, 4, 18, 14),
          child: Row(
            children: [
              Expanded(
                flex: 10,
                child: DragTarget<TripItem>(
                  onAcceptWithDetails: (d) => _drop(store, t, d.data, null),
                  builder: (context, cand, _) => AnimatedScale(
                    duration: const Duration(milliseconds: 150),
                    scale: cand.isNotEmpty ? 1.06 : 1,
                    child: PassButton(
                      dragging ? '담은 곳으로' : '담은 곳 ${t.wish.length}',
                      primary: cand.isNotEmpty,
                      onTap: () => showWishSheet(context, t.id, defaultDay: _day),
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(flex: 14, child: PassButton('+ 추가', primary: true, onTap: () => _addSheet(context, t))),
            ],
          ),
        ),
      ],
    );
  }

  Widget _dayChip(Store store, Trip t, int i) {
    final sel = i == _day;
    final date = t.dateOf(i);
    return DragTarget<TripItem>(
      onWillAcceptWithDetails: (_) => true,
      onAcceptWithDetails: (d) => _drop(store, t, d.data, i),
      builder: (context, cand, _) {
        final over = cand.isNotEmpty;
        return Press(
          onTap: () => setState(() => _day = i),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 180),
            width: 58,
            margin: EdgeInsets.only(bottom: sel || over ? 0 : 4),
            transform: over ? Matrix4.diagonal3Values(1.08, 1.08, 1) : Matrix4.identity(),
            transformAlignment: Alignment.center,
            decoration: BoxDecoration(
              color: over ? C.red : (sel ? C.paper : Colors.transparent),
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: over ? C.red : (sel ? C.ink : C.line2), width: 1.5),
              boxShadow: sel || over ? const [BoxShadow(color: C.ink, offset: Offset(0, 4))] : null,
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text('D${i + 1}', style: mono(14, bold: true, c: over ? Colors.white : (sel ? C.ink : C.muted))),
                Text('${two(date.day)} ${wkEn(date)}', style: mono(9.5, c: over ? Colors.white : (sel ? C.ink : C.faint))),
              ],
            ),
          ),
        );
      },
    );
  }

  void _drop(Store store, Trip t, TripItem item, int? day, [int? index]) {
    HapticFeedback.mediumImpact();
    final from = store.findDay(t, item);
    store.moveTo(t, item, day, index);
    setState(() => _dragging = null);
    if (day != from) {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(
          SnackBar(
            content: Text(day == null ? '${item.title} → 담은 곳' : '${item.title} → D${day + 1} ${md(t.dateOf(day))}'),
            action: SnackBarAction(
              label: '되돌리기',
              textColor: const Color(0xFFFF8A8C),
              onPressed: () => store.moveTo(t, item, from),
            ),
          ),
        );
    }
  }

  void _addSheet(BuildContext context, Trip t) {
    final leg = t.legOf(_day);
    showPassSheet(
      context,
      (c) => SizedBox(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(18, 0, 18, 18),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('D${_day + 1}에 넣기', style: sans(18, w: FontWeight.w800)),
              const SizedBox(height: 12),
              _addOption(c, Icons.travel_explore_rounded, '장소 찾기', '${leg?.cityKo ?? ''} 관광지에서 담기', () {
                Navigator.push(context, slideRoute(GuideScreen(tripId: t.id, destId: leg?.destId ?? '', day: _day)));
              }),
              GridView.count(
                crossAxisCount: 3,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                mainAxisSpacing: 10,
                crossAxisSpacing: 10,
                childAspectRatio: 1.25,
                padding: const EdgeInsets.only(top: 10),
                children: [
                  for (final type in [
                    ItemType.flight,
                    ItemType.stay,
                    ItemType.food,
                    ItemType.shopping,
                    ItemType.place,
                    ItemType.memo,
                  ])
                    _typeTile(
                      c,
                      type,
                      () => Navigator.push(context, slideRoute(ItemEditScreen(tripId: t.id, newType: type, day: _day))),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _addOption(BuildContext sheet, IconData icon, String title, String sub, VoidCallback onTap) => Press(
    onTap: () {
      Navigator.pop(sheet);
      onTap();
    },
    child: Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(color: C.ink, borderRadius: BorderRadius.circular(14)),
      child: Row(
        children: [
          Icon(icon, color: Colors.white),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: sans(15, w: FontWeight.w800, c: Colors.white),
                ),
                Text(sub, style: sans(12, c: C.onInk)),
              ],
            ),
          ),
          const Icon(Icons.chevron_right_rounded, color: C.onInk),
        ],
      ),
    ),
  );

  Widget _typeTile(BuildContext sheet, ItemType type, VoidCallback onTap) => Press(
    onTap: () {
      Navigator.pop(sheet);
      onTap();
    },
    child: Container(
      decoration: BoxDecoration(
        color: C.paper,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: C.ink, width: 1.5),
        boxShadow: const [BoxShadow(color: C.ink, offset: Offset(0, 3))],
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(itemIcon(type), color: C.ink),
          const SizedBox(height: 4),
          Text(type == ItemType.place ? '직접 장소' : type.label, style: sans(13, w: FontWeight.w800)),
        ],
      ),
    ),
  );
}

class _Tabs extends StatelessWidget {
  final List<String> labels;
  final int index;
  final ValueChanged<int> onTap;
  const _Tabs({required this.labels, required this.index, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 44,
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: C.line2, width: 1.5),
      ),
      child: LayoutBuilder(
        builder: (context, box) {
          final w = box.maxWidth / labels.length;
          return Stack(
            children: [
              AnimatedPositioned(
                duration: const Duration(milliseconds: 240),
                curve: Curves.easeOutBack,
                left: w * index,
                top: 0,
                bottom: 0,
                width: w,
                child: Container(
                  decoration: BoxDecoration(color: C.ink, borderRadius: BorderRadius.circular(9)),
                ),
              ),
              Row(
                children: [
                  for (var i = 0; i < labels.length; i++)
                    Expanded(
                      child: GestureDetector(
                        behavior: HitTestBehavior.opaque,
                        onTap: () => onTap(i),
                        child: Center(
                          child: AnimatedDefaultTextStyle(
                            duration: const Duration(milliseconds: 200),
                            style: sans(14, w: FontWeight.w800, c: i == index ? Colors.white : C.muted),
                            child: Text(labels[i]),
                          ),
                        ),
                      ),
                    ),
                ],
              ),
            ],
          );
        },
      ),
    );
  }
}
