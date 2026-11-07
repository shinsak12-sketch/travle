import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'data/store.dart';
import 'screens/home_screen.dart';
import 'ui/theme.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    statusBarIconBrightness: Brightness.dark,
    systemNavigationBarColor: C.bg,
    systemNavigationBarIconBrightness: Brightness.dark,
  ));
  final store = Store()..init();
  runApp(StoreScope(store: store, child: const TravleApp()));
}

class TravleApp extends StatelessWidget {
  const TravleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '트래블',
      debugShowCheckedModeBanner: false,
      theme: buildTheme(),
      locale: const Locale('ko', 'KR'),
      supportedLocales: const [Locale('ko', 'KR')],
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      home: const _Boot(),
    );
  }
}

class _Boot extends StatelessWidget {
  const _Boot();
  @override
  Widget build(BuildContext context) {
    final store = StoreScope.of(context);
    if (!store.ready) {
      return Scaffold(
        body: Center(
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            Text('TRAVLE', style: mono(28, bold: true, ls: 6)),
            const SizedBox(height: 8),
            const Eyebrow('LOADING 2,263 CITIES'),
          ]),
        ),
      );
    }
    return const HomeScreen();
  }
}
