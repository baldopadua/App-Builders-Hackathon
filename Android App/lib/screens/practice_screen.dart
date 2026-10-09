import 'package:flutter/material.dart';
import '../services/platform_bridge.dart';
import 'secrets_screen.dart';

class PracticeScreen extends StatefulWidget {
  const PracticeScreen({super.key});

  @override
  State<PracticeScreen> createState() => _PracticeScreenState();
}

class _PracticeScreenState extends State<PracticeScreen> {
  List<PrivateSecret> _secrets = [];
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _loadSecrets();
  }

  Future<void> _loadSecrets() async {
    setState(() => _isLoading = true);
    final secrets = await PlatformBridge.getSecrets();
    setState(() {
      _secrets = secrets;
      _isLoading = false;
    });
  }

  Future<void> _showAddSecretDialog() async {
    final labelController = TextEditingController();
    final valueController = TextEditingController();

    await showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Add Private Note'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: labelController,
                decoration: const InputDecoration(
                  labelText: 'Label (e.g. Secret PIN, Nickname)',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: valueController,
                decoration: const InputDecoration(
                  labelText: 'Secret Word / Code',
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: () async {
                final label = labelController.text.trim();
                final value = valueController.text.trim();
                if (label.isNotEmpty && value.isNotEmpty) {
                  await PlatformBridge.addSecret(label: label, value: value);
                  if (context.mounted) Navigator.pop(context);
                  _loadSecrets();
                }
              },
              child: const Text('Protect'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _deleteSecret(String id) async {
    await PlatformBridge.deleteSecret(id);
    _loadSecrets();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('SecondLook Practice'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        actions: [
          IconButton(
            icon: const Icon(Icons.shield_outlined),
            tooltip: "Teach Me What's Private",
            onPressed: () {
              Navigator.of(context)
                  .push(
                    MaterialPageRoute(
                      builder: (context) => const SecretsScreen(),
                    ),
                  )
                  .then((_) => _loadSecrets());
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              '⌨️ Test Typing Area',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            const TextField(
              decoration: InputDecoration(
                border: OutlineInputBorder(),
                labelText: 'Type here using SecondLook Keyboard...',
                hintText: 'e.g. My password is hunter2',
              ),
              maxLines: 3,
            ),
            const SizedBox(height: 24),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  '🛡️ Private Notes Vault',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                FilledButton.icon(
                  onPressed: _showAddSecretDialog,
                  icon: const Icon(Icons.add, size: 18),
                  label: const Text('Add Note'),
                ),
              ],
            ),
            const SizedBox(height: 8),
            const Text(
              'Anything added here is saved with Keystore encryption. If you type it in the keyboard, the AI warns you instantly!',
              style: TextStyle(color: Colors.grey, fontSize: 13),
            ),
            const SizedBox(height: 12),
            if (_isLoading)
              const Center(child: Padding(
                padding: EdgeInsets.all(16.0),
                child: CircularProgressIndicator(),
              ))
            else if (_secrets.isEmpty)
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.grey.shade100,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Text(
                  'No private notes yet. Tap "Add Note" to protect a secret word or code.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.black54),
                ),
              )
            else
              ListView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: _secrets.length,
                itemBuilder: (context, index) {
                  final secret = _secrets[index];
                  return Card(
                    margin: const EdgeInsets.symmetric(vertical: 4),
                    child: ListTile(
                      leading: const Icon(Icons.lock, color: Colors.deepPurple),
                      title: Text(secret.label, style: const TextStyle(fontWeight: FontWeight.w600)),
                      subtitle: Text('Value: •••••••• (Protected)'),
                      trailing: IconButton(
                        icon: const Icon(Icons.delete_outline, color: Colors.red),
                        onPressed: () => _deleteSecret(secret.id),
                      ),
                    ),
                  );
                },
              ),
          ],
        ),
      ),
    );
  }
}
