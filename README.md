# Nativeblocks compiler

The Nativeblocks compiler is a tool that generates server-driven blocks and actions based on Kotlin
code. It produces
JSON and Kotlin files for each block and action, preparing them for upload to Nativeblocks servers.

## How it works

Before starting we need to add necessary dependencies

[Add KSP](https://kotlinlang.org/docs/ksp-quickstart.html#add-a-processor)

```groovy
implementation("io.nativeblocks:nativeblocks-android:[latest-version]")
implementation("io.nativeblocks:nativeblocks-compiler-android:[latest-version]")
ksp("io.nativeblocks:nativeblocks-compiler-android:[latest-version]")
```

### [How block it works on Block](/docs/block.md)

### [How block it works on Action](/docs/action.md)

### Usage

After providing annotations for blocks and actions, the project generates Kotlin code and
corresponding JSON files upon
building. These can then be initialize in MainActivity or via dependency injection

```kotlin
AppBlockProvider.provideBlocks()
AppActionProvider.provideActions(xBot)
```

For actions, all dependencies must be provided during initialization. To optimize performance,
consider using dependency
injection for scoped or lazy instances.