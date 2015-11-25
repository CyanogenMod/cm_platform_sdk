## CMSettings Migration Test
The tests host library contains a simple interface which calls down to a client interface on the device
to take a snapshot of the current settings, forces an update with the current build defined in params,
then verifies the settings post migration.

To run the test (on a live device):

  ``` java -cp /Volumes/CM/CM13/out/host/darwin-x86/framework/migration-interface.jar MigrationTest \
  --settings <example settings> --bootimg <boot.img> --systemimg <system.img> ```

To generate example settings to be written against 12.1 device during migration:

  ```java -cp /Volumes/CM/CM13/out/host/<platform>/framework/migration-interface.jar GenerateExampleSettings <output file path> ```
