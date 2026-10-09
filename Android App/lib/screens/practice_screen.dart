import 'package:flutter/material.dart';

class PracticeScreen extends StatelessWidget {
  const PracticeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('SecondLook Practice'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              'How to test your new keyboard:',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            const Text(
              '1. Go to your Android Settings.\n'
              '2. Search for "Manage on-screen keyboards" (usually under System > Keyboard).\n'
              '3. Toggle on "SecondLook Keyboard".\n'
              '4. Come back here, tap the text field below, and change your active keyboard to SecondLook.',
              style: TextStyle(fontSize: 16),
            ),
            const SizedBox(height: 32),
            TextField(
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
                labelText: 'Type a message...',
                hintText: 'e.g. My password is hunter2',
              ),
              maxLines: 5,
            ),
          ],
        ),
      ),
    );
  }
}
