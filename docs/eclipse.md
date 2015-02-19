# Eclipse Usage

To fully use Nextop as an Android Library, include the .project as a dependent
project in Eclipse. [SEE #2](https://github.com/nextopio/nextop-client/issues/2)

# JAR Usage Caveats

In cases where you must include Nextop as a JAR dependency (even
manually downloading the JARs and including them in a libs/ dir),
the following won't work:

- Nextop custom fragments, such as DebugFragment or RecordFragment

The following will work:

- Core network stack
- ImageView

[Please submit issues if you hit integration problems](https://github.com/nextopio/nextop-client/issues)

