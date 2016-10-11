package org.jenkinsci.plugins.environmentdashboard;

import hudson.Extension;
import javaposse.jobdsl.dsl.helpers.wrapper.WrapperContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;

@Extension(optional = true)
public class EnvDashboardDslContextExtension extends ContextExtensionPoint {

    /*
    DSL extension method to handle the nested/empty environment dashboard wrapper context.

    Example 1: Nested context
    environmentDashboard {
      nameOfEnv('EnvA')
      componentName('ComponentB')
      buildNumber('1')
    }

    Example 2: Empty context
    environmentDashboard {}
     */
    @DslExtensionMethod(context = WrapperContext.class)
    public Object environmentDashboard(Runnable closure) {
        EnvDashboardDslContext context = new EnvDashboardDslContext();
        executeInContext(closure, context);
        return new DashboardBuilder(context.nameOfEnv, context.componentName, context.buildNumber,
                context.buildJob, context.packageName, context.addColumns, context.data);
    }

    /*
    DSL extension method to handle non-existent closure environment dashboard wrapper context.

    Example:
    environmentDashboard()
     */
    @DslExtensionMethod(context = WrapperContext.class)
    public Object environmentDashboard() {
        EnvDashboardDslContext context = new EnvDashboardDslContext();
        return new DashboardBuilder(context.nameOfEnv, context.componentName, context.buildNumber,
                context.buildJob, context.packageName, context.addColumns, context.data);
    }
}
