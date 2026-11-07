import 'package:flutter/material.dart';

/// 화면 전환: 오른쪽에서 밀려 들어옴
Route<T> slideRoute<T>(Widget page) => PageRouteBuilder<T>(
      transitionDuration: const Duration(milliseconds: 320),
      reverseTransitionDuration: const Duration(milliseconds: 260),
      pageBuilder: (_, _, _) => page,
      transitionsBuilder: (_, a, _, child) {
        final c = CurvedAnimation(parent: a, curve: Curves.easeOutCubic);
        return SlideTransition(
          position: Tween(begin: const Offset(1, 0), end: Offset.zero).animate(c),
          child: child,
        );
      },
    );
