// 예산: 총예산 대비 지출, 분류별 합계, 환율, 지출 내역
import 'package:flutter/material.dart';

import '../data/models.dart';
import '../data/store.dart';
import '../ui/sheet.dart';
import '../ui/theme.dart';
import '../util/format.dart';
import 'item_edit_screen.dart' show currencies;

const expenseCats = ['항공', '숙소', '식비', '교통', '관광', '쇼핑', '기타'];
const _catColors = [C.ink, Color(0xFF3B5BA9), C.red, Color(0xFF1F8A5B), Color(0xFFB0730C), Color(0xFF8A4FBF), C.faint];

class BudgetTab extends StatelessWidget {
  final Trip trip;
  const BudgetTab({super.key, required this.trip});

  @override
  Widget build(BuildContext context) {
    final s = StoreScope.of(context);
    final t = trip;
    final spent = t.spent;
    final left = t.budget - spent;
    final ratio = t.budget == 0 ? 0.0 : (spent / t.budget).clamp(0.0, 1.0);
    final byCat = {for (final c in expenseCats) c: 0.0};
    for (final e in t.expenses) {
      byCat[e.category] = (byCat[e.category] ?? 0) + t.toKrw(e.amount, e.currency);
    }
    final used = {for (final e in t.expenses) e.currency}..remove('KRW');
    final rateKeys = {...t.rates.keys, ...used}.toList()..sort();
    final missing = rateKeys.where((k) => (t.rates[k] ?? 0) == 0).toList();
    final list = [...t.expenses]..sort((a, b) => b.date.compareTo(a.date));

    return ListView(
      padding: const EdgeInsets.fromLTRB(18, 12, 18, 24),
      children: [
        // 요약 탑승권
        Container(
          decoration: BoxDecoration(
            color: C.paper,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: C.ink, width: 1.5),
            boxShadow: const [BoxShadow(color: C.ink, offset: Offset(0, 4))],
          ),
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 14, 16, 12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Eyebrow('SPENT'),
                        const Spacer(),
                        Press(
                          onTap: () => _editBudget(context, s),
                          child: Text(
                            t.budget == 0 ? '예산 정하기' : '예산 ${won(t.budget)}',
                            style: sans(12.5, c: C.muted, w: FontWeight.w800).copyWith(decoration: TextDecoration.underline),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(won(spent), style: mono(28, bold: true)),
                    if (t.budget > 0) ...[
                      const SizedBox(height: 10),
                      ClipRRect(
                        borderRadius: BorderRadius.circular(4),
                        child: TweenAnimationBuilder<double>(
                          tween: Tween(end: ratio),
                          duration: const Duration(milliseconds: 500),
                          curve: Curves.easeOutCubic,
                          builder: (_, v, _) => LinearProgressIndicator(
                            value: v,
                            minHeight: 10,
                            color: left < 0 ? C.red : C.ink,
                            backgroundColor: C.line,
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              if (t.budget > 0) ...[
                const Dashed(),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
                  child: Row(
                    children: [
                      Text(left < 0 ? 'OVER' : 'LEFT', style: mono(11, c: left < 0 ? C.red : C.faint)),
                      const SizedBox(width: 8),
                      Text(won(left.abs()), style: mono(16, bold: true, c: left < 0 ? C.red : C.ink)),
                      const Spacer(),
                      Text('${(spent / t.budget * 100).round()}%', style: mono(13, bold: true, c: C.muted)),
                    ],
                  ),
                ),
              ],
            ],
          ),
        ),
        if (missing.isNotEmpty) ...[
          const SizedBox(height: 10),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: C.redSoft,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: C.red),
            ),
            child: Text('${missing.join(', ')} 환율이 없어서 합계에서 빠져 있음. 아래 환율에 넣어줘.', style: sans(13, c: C.red)),
          ),
        ],
        const SizedBox(height: 16),
        // 분류별
        if (spent > 0) ...[
          const Eyebrow('BY CATEGORY'),
          const SizedBox(height: 8),
          ClipRRect(
            borderRadius: BorderRadius.circular(6),
            child: SizedBox(
              height: 14,
              child: Row(
                children: [
                  for (var i = 0; i < expenseCats.length; i++)
                    if ((byCat[expenseCats[i]] ?? 0) > 0)
                      Expanded(
                        flex: ((byCat[expenseCats[i]]! / spent) * 1000).round().clamp(1, 1000),
                        child: Container(color: _catColors[i]),
                      ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 12,
            runSpacing: 4,
            children: [
              for (var i = 0; i < expenseCats.length; i++)
                if ((byCat[expenseCats[i]] ?? 0) > 0)
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Container(
                        width: 8,
                        height: 8,
                        decoration: BoxDecoration(color: _catColors[i], shape: BoxShape.circle),
                      ),
                      const SizedBox(width: 4),
                      Text('${expenseCats[i]} ', style: sans(12.5, c: C.muted)),
                      Text(won(byCat[expenseCats[i]]!), style: mono(12, bold: true)),
                    ],
                  ),
            ],
          ),
          const SizedBox(height: 16),
        ],
        // 환율
        Row(
          children: [
            const Eyebrow('RATES · 1단위 = 원'),
            const Spacer(),
            TextButton(
              onPressed: () => _editRate(context, s, null),
              child: Text('+ 통화', style: sans(13, w: FontWeight.w800)),
            ),
          ],
        ),
        if (rateKeys.isEmpty)
          Text('외화로 쓰면 여기서 환율을 넣음 (예: JPY 9.2)', style: sans(13, c: C.muted))
        else
          Wrap(
            spacing: 6,
            runSpacing: 6,
            children: [
              for (final k in rateKeys)
                PassChip(
                  '$k ${(t.rates[k] ?? 0) == 0 ? '?' : comma(t.rates[k]!, digits: t.rates[k]! < 100 ? 2 : 0)}',
                  selected: (t.rates[k] ?? 0) == 0,
                  selectedColor: C.red,
                  onTap: () => _editRate(context, s, k),
                ),
            ],
          ),
        const SizedBox(height: 18),
        // 내역
        Row(
          children: [
            Text('지출 내역', style: sans(15, w: FontWeight.w800)),
            const SizedBox(width: 6),
            Text('${list.length}', style: mono(12, c: C.faint)),
          ],
        ),
        const SizedBox(height: 8),
        if (list.isEmpty) const EmptyNote('아직 지출이 없음', '아래 "+ 지출"로 적거나, 일정 카드(항공·숙소·맛집 등)에 금액을 넣으면 자동으로 들어옴.'),
        for (var i = 0; i < list.length; i++)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: SlideIn(index: i, child: _expenseRow(context, s, list[i])),
          ),
        const SizedBox(height: 8),
        PassButton('+ 지출', primary: true, onTap: () => _editExpense(context, s, null)),
      ],
    );
  }

  Widget _expenseRow(BuildContext context, Store s, Expense e) {
    final i = expenseCats.indexOf(e.category);
    return Press(
      onTap: () => _editExpense(context, s, e),
      child: Container(
        padding: const EdgeInsets.fromLTRB(12, 10, 14, 10),
        decoration: BoxDecoration(color: C.paper, borderRadius: BorderRadius.circular(12)),
        child: Row(
          children: [
            Container(
              width: 6,
              height: 34,
              decoration: BoxDecoration(color: _catColors[i < 0 ? 6 : i], borderRadius: BorderRadius.circular(3)),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    e.title,
                    style: sans(14.5, w: FontWeight.w800),
                    overflow: TextOverflow.ellipsis,
                  ),
                  Text('${md(e.date)} · ${e.category}${e.itemId != null ? ' · 일정 카드' : ''}', style: sans(12, c: C.muted)),
                ],
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(money(e.amount, e.currency), style: mono(14, bold: true)),
                if (e.currency != 'KRW' && (trip.rates[e.currency] ?? 0) > 0)
                  Text('≈ ${won(trip.toKrw(e.amount, e.currency))}', style: mono(11, c: C.faint)),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _editBudget(BuildContext context, Store s) async {
    final c = TextEditingController(text: trip.budget == 0 ? '' : comma(trip.budget));
    final ok = await _sheet(context, '총예산', [
      TextField(
        controller: c,
        autofocus: true,
        keyboardType: TextInputType.number,
        inputFormatters: const [ThousandsFormatter(decimal: false)],
        style: mono(18, bold: true),
        decoration: const InputDecoration(suffixText: '원'),
      ),
    ]);
    if (ok) {
      trip.budget = parseAmount(c.text)?.round() ?? 0;
      s.changed();
    }
    c.dispose();
  }

  Future<void> _editRate(BuildContext context, Store s, String? cur) async {
    var code = cur ?? 'JPY';
    final c = TextEditingController(text: cur == null || (trip.rates[cur] ?? 0) == 0 ? '' : amountText(trip.rates[cur]));
    final ok = await _sheet(context, '환율', [
      if (cur == null)
        StatefulBuilder(
          builder: (_, set) => Wrap(
            spacing: 6,
            runSpacing: 6,
            children: [
              for (final k in currencies.where((k) => k != 'KRW'))
                PassChip(k, selected: code == k, onTap: () => set(() => code = k)),
            ],
          ),
        ),
      const SizedBox(height: 12),
      TextField(
        controller: c,
        autofocus: true,
        keyboardType: const TextInputType.numberWithOptions(decimal: true),
        inputFormatters: const [ThousandsFormatter()],
        style: mono(18, bold: true),
        decoration: const InputDecoration(labelText: '1단위 = 몇 원 (예: JPY 9.2, USD 1,380)'),
      ),
    ]);
    if (ok) {
      final v = parseAmount(c.text);
      if (v != null) trip.rates[code] = v;
      s.changed();
    }
    c.dispose();
  }

  Future<void> _editExpense(BuildContext context, Store s, Expense? e) async {
    if (e?.itemId != null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('일정 카드에서 만든 지출은 그 카드에서 금액을 고쳐줘')));
      return;
    }
    final title = TextEditingController(text: e?.title ?? '');
    final amt = TextEditingController(text: e == null ? '' : amountText(e.amount));
    var cat = e?.category ?? '식비';
    var cur = e?.currency ?? (trip.rates.keys.firstOrNull ?? 'KRW');
    var date = e?.date ?? (trip.todayIndex() != null ? DateTime.now() : trip.start);
    var delete = false;
    final ok = await _sheet(context, e == null ? '지출 추가' : '지출', [
      StatefulBuilder(
        builder: (ctx, set) => Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            TextField(
              controller: title,
              autofocus: e == null,
              style: sans(15),
              decoration: const InputDecoration(labelText: '내용 (예: 이치란 라멘)'),
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: amt,
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    inputFormatters: const [ThousandsFormatter()],
                    style: mono(17, bold: true),
                    decoration: const InputDecoration(labelText: '금액'),
                  ),
                ),
                const SizedBox(width: 8),
                DropdownButton<String>(
                  value: cur,
                  style: mono(14, bold: true),
                  items: [
                    for (final c in {...currencies, ...trip.rates.keys}) DropdownMenuItem(value: c, child: Text(c)),
                  ],
                  onChanged: (v) => set(() => cur = v ?? 'KRW'),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [for (final c in expenseCats) PassChip(c, selected: cat == c, onTap: () => set(() => cat = c))],
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                TextButton.icon(
                  onPressed: () async {
                    final d = await showDatePicker(
                      context: ctx,
                      initialDate: date,
                      firstDate: DateTime(2020),
                      lastDate: DateTime(2040),
                    );
                    if (d != null) set(() => date = d);
                  },
                  icon: const Icon(Icons.event_rounded, size: 18),
                  label: Text(ymd(date), style: mono(14, bold: true)),
                ),
                const Spacer(),
                if (e != null)
                  TextButton(
                    onPressed: () {
                      delete = true;
                      Navigator.pop(ctx, true);
                    },
                    child: Text(
                      '삭제',
                      style: sans(14, c: C.red, w: FontWeight.w800),
                    ),
                  ),
              ],
            ),
          ],
        ),
      ),
    ]);
    if (ok) {
      if (delete && e != null) {
        trip.expenses.remove(e);
        if (context.mounted) {
          ScaffoldMessenger.of(context)
            ..hideCurrentSnackBar()
            ..showSnackBar(
              SnackBar(
                content: Text('"${e.title}" 지움'),
                action: SnackBarAction(
                  label: '되돌리기',
                  textColor: const Color(0xFFFF8A8C),
                  onPressed: () {
                    trip.expenses.add(e);
                    s.changed();
                  },
                ),
              ),
            );
        }
      } else {
        final v = parseAmount(amt.text);
        if (v != null && title.text.trim().isNotEmpty) {
          if (cur != 'KRW' && !trip.rates.containsKey(cur)) trip.rates[cur] = 0;
          if (e == null) {
            trip.expenses.add(Expense(title: title.text.trim(), category: cat, amount: v, currency: cur, date: date));
          } else {
            e
              ..title = title.text.trim()
              ..category = cat
              ..amount = v
              ..currency = cur
              ..date = date;
          }
        }
      }
      s.changed();
    }
    title.dispose();
    amt.dispose();
  }

  Future<bool> _sheet(BuildContext context, String title, List<Widget> body) async {
    final r = await showPassSheet<bool>(
      context,
      (c) => Padding(
        padding: const EdgeInsets.fromLTRB(18, 0, 18, 18),
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: sans(18, w: FontWeight.w800)),
              const SizedBox(height: 12),
              ...body,
              const SizedBox(height: 16),
              PassButton('저장', primary: true, onTap: () => Navigator.pop(c, true)),
            ],
          ),
        ),
      ),
    );
    return r == true;
  }
}
