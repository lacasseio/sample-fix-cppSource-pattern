# Demonstrate how to fix `CppComponent#cppSource` include pattern

Unfortunately, Gradle restricts the C++ source files to a specific pattern, which can often be wrong.
This sample demonstrates how to "shadow" the `cppSource` to include additional source patterns.
It's essential to remember that when using this _shadowing_ technique, all references to the `cppSource` property must check for the _shadowed_ property in a lazy way.

