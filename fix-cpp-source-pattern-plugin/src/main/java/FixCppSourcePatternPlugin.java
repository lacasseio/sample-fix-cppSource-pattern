import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.language.cpp.CppBinary;
import org.gradle.language.cpp.CppComponent;
import org.gradle.language.cpp.tasks.CppCompile;

import java.util.concurrent.Callable;

public abstract class FixCppSourcePatternPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getComponents().withType(CppComponent.class).configureEach(component -> {
            ((ExtensionAware) component).getExtensions().getExtraProperties().set("cppSource", project.getObjects().fileCollection().from(cppSourceOf(component)).from((Callable<Object>) () -> {
                FileTree tree;
                if (component.getSource().getFrom().isEmpty()) {
                    tree = project.getLayout().getProjectDirectory().dir("src/" + component.getName() + "/cpp").getAsFileTree();
                } else {
                    tree = component.getSource().getAsFileTree();
                }
                return tree.matching(it -> it.include("**/*.cxx"));
            }));
            component.getBinaries().configureEach(CppBinary.class, binary -> {
                project.getTasks().named(compileTaskName(binary), CppCompile.class).configure(task -> {
                    try {
                        task.getSource().from((Callable<?>) () -> cppSourceOf(component));
                    } catch (IllegalStateException e) {
                        // We only log the failure as the `cppSource` may be wired through a different process
                        //   See per-source file compiler args sample.
                        project.getLogger().info(String.format("Could not wire shadowed 'cppSource' from %s in %s to %s.", component, project, task));
                    }
                });
            });
        });
    }

    private static FileCollection cppSourceOf(CppComponent component) {
        FileCollection result = null;
        if (component instanceof ExtensionAware) {
            result = (FileCollection) ((ExtensionAware) component).getExtensions().getExtraProperties().getProperties().get("cppSource");
        }

        if (result == null) {
            result = component.getCppSource();
        }

        return result;
    }

    //region Names
    private static String qualifyingName(CppBinary binary) {
        String result = binary.getName();
        if (result.startsWith("main")) {
            result = result.substring("main".length());
        } else if (result.endsWith("Executable")) {
            result = result.substring(0, result.length() - "Executable".length());
        }
        return uncapitalize(result);
    }

    private static String compileTaskName(CppBinary binary) {
        return "compile" + capitalize(qualifyingName(binary)) + "Cpp";
    }
    //endregion

    //region StringUtils
    private static String uncapitalize(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    //endregion
}
