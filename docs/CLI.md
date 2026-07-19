## CLI Documentation

GitDown can be launched directly against a repository by passing the
repository's path as the first command line argument. If the path points to
a valid git repository, GitDown will open it immediately instead of showing
the directory selection screen.

#### Usage

```
./git-down <path-to-repository>
```

For example, to open a repository checked out at `~/projects/git-down`:

```
./git-down ~/projects/git-down
```

While developing, you can pass the argument through Gradle:

```
./gradlew run --args="/path/to/repository"
```

Only a single path argument is supported. If no argument is given, or more
than one is given, GitDown falls back to the directory selection screen.
