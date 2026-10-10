import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:secondlook/screens/practice_screen.dart';
import 'package:secondlook/screens/secrets_screen.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const MethodChannel channel = MethodChannel('secondlook/ai');

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  testWidgets('Clicking Private note when no notes exist prompts user to create one',
      (WidgetTester tester) async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
      if (methodCall.method == 'getSecrets') {
        return [];
      }
      return null;
    });

    await tester.pumpWidget(const MaterialApp(home: PracticeScreen()));
    await tester.pumpAndSettle();

    // Verify "Private note" option exists
    final privateNoteOption = find.text('Private note');
    expect(privateNoteOption, findsOneWidget);

    // Tap "Private note"
    await tester.tap(privateNoteOption);
    await tester.pumpAndSettle();

    // Verify dialog prompt appears
    expect(find.text('No private notes yet'), findsOneWidget);
    expect(
      find.text(
        'Add a private note first so Tanggolin can practice flagging your sensitive details in drafts.',
      ),
      findsOneWidget,
    );
    expect(find.text('Create note'), findsOneWidget);

    // Tap "Create note" should navigate to SecretsScreen
    await tester.tap(find.text('Create note'));
    await tester.pumpAndSettle();

    expect(find.byType(SecretsScreen), findsOneWidget);
  });

  testWidgets('Clicking Private note when notes exist picks the first note value',
      (WidgetTester tester) async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
      if (methodCall.method == 'getSecrets') {
        return [
          {
            'id': 'note_1',
            'label': 'Secret Project',
            'value': 'Project Starlight classified launch',
            'createdAt': 1600000000000,
          },
          {
            'id': 'note_2',
            'label': 'Second Note',
            'value': 'Another confidential detail',
            'createdAt': 1600000001000,
          },
        ];
      }
      return null;
    });

    await tester.pumpWidget(const MaterialApp(home: PracticeScreen()));
    await tester.pumpAndSettle();

    // Tap "Private note"
    await tester.tap(find.text('Private note'));
    await tester.pumpAndSettle();

    // Test message field should contain the first note's value
    expect(find.text('Project Starlight classified launch'), findsOneWidget);
  });
}
