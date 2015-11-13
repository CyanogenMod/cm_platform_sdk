## CMSettings Migration Test
The tests host library contains a simple interface which calls down to a client jar on the device
to take a snapshot of the current settings, forces an update with the current build in the tree,
then verifies the settings post migration.

To run the test (on a live device):
  
  ```java -cp /Volumes/CM/CM13/out/host/<platform>/framework/migration-interface.jar MigrationTest```
