/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts;

import org.gradle.api.Action;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.component.ComponentSelector;
import org.gradle.api.artifacts.result.ComponentSelectionReason;
import org.gradle.api.internal.DelegatingDomainObjectSet;
import org.gradle.api.internal.tasks.AbstractTaskDependency;
import org.gradle.api.internal.tasks.TaskDependencyResolveContext;
import org.gradle.api.tasks.TaskDependency;

import java.util.HashSet;
import java.util.Set;

public class SubstitutingDependencySet extends DelegatingDomainObjectSet<Dependency> implements DependencySet {
    private final TaskDependency builtBy = new DependencySetTaskDependency();
    private final String displayName;
    private final DependencyResolveRuleProvider resolveRuleProvider;

    public SubstitutingDependencySet(String displayName, DomainObjectSet<Dependency> backingSet, DependencyResolveRuleProvider resolveRuleProvider) {
        super(backingSet);
        this.displayName = displayName;
        this.resolveRuleProvider = resolveRuleProvider;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public TaskDependency getBuildDependencies() {
        return builtBy;
    }

    // The real implementation is DefaultDependencyResolveDetails in dependencyManagement
    // We could create a simple implementation here to record the changes,
    // but unfortunately ModuleVersionSelector and ComponentSelector implementations
    // are also in dependencyManagement, so they must be either moved to core or
    // implemented here on top of Dependency somehow.
    private class SimpleDependencyResolveDetails<T extends ComponentSelector> implements DependencyResolveDetailsInternal<T> {

        private final Dependency dependency;

        public SimpleDependencyResolveDetails(Dependency dependency) {

            this.dependency = dependency;
        }

        @Override
        public T getSelector() {
            return null;
        }

        @Override
        public ModuleVersionSelector getRequested() {
            return null;
        }

        @Override
        public void useVersion(String version) {

        }

        @Override
        public void useTarget(Object notation) {

        }

        @Override
        public ComponentSelector getTarget() {
            return null;
        }

        @Override
        public void useVersion(String version, ComponentSelectionReason selectionReason) {

        }

        @Override
        public ComponentSelectionReason getSelectionReason() {
            return null;
        }

        @Override
        public boolean isUpdated() {
            return false;
        }
    }

    private class DependencySetTaskDependency extends AbstractTaskDependency {
        @Override
        public String toString() {
            return String.format("build dependencies %s", SubstitutingDependencySet.this);
        }

        public void resolve(TaskDependencyResolveContext context) {
            Set<SelfResolvingDependency> selfResolvingDependencies = new HashSet<SelfResolvingDependency>();
            Action<DependencyResolveDetailsInternal> resolveRule = resolveRuleProvider.getDependencyResolveRule();

            for (Dependency dependency : SubstitutingDependencySet.this) {
                SimpleDependencyResolveDetails<? extends ComponentSelector> resolveDetails =
                        new SimpleDependencyResolveDetails<ComponentSelector>(dependency);
                resolveRule.execute(resolveDetails);

                Dependency substitutedDependency = dependency; // TODO

                if (substitutedDependency instanceof SelfResolvingDependency) {
                    selfResolvingDependencies.add((SelfResolvingDependency) substitutedDependency);
                }
            }

            for (SelfResolvingDependency dependency : selfResolvingDependencies) {
                context.add(dependency);
            }
        }
    }
}
