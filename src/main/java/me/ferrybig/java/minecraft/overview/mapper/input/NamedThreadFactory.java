/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.java.minecraft.overview.mapper.input;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NamedThreadFactory implements ThreadFactory {

	@Nullable
	private final ThreadGroup group;
	@Nonnull
	private final AtomicInteger number = new AtomicInteger();
	@Nonnull
	private final String namePrefix;

	public NamedThreadFactory(@Nonnull String namePrefix) {
		this(null, namePrefix);
	}

	public NamedThreadFactory(@Nullable ThreadGroup parent, @Nonnull String namePrefix) {
		this.namePrefix = Objects.requireNonNull(namePrefix, "namePrefix");
		this.group = parent == null ? new ThreadGroup(namePrefix) : new ThreadGroup(parent, namePrefix);
	}

	@Override
	@Nonnull
	public Thread newThread(@Nonnull Runnable r) {
		return new Thread(group, r, namePrefix + "-" + number.incrementAndGet());
	}

	@Nonnull
	public static ThreadFactory lazyFactory(@Nonnull String namePrefix) {
		Objects.requireNonNull(namePrefix, "namePrefix");
		return new ThreadFactory() {
			private volatile ThreadFactory instance;

			@Override
			@Nonnull
			public Thread newThread(@Nonnull Runnable r) {
				@SuppressWarnings("LocalVariableHidesMemberVariable")
				ThreadFactory instance = this.instance;
				if (instance == null) {
					synchronized (this) {
						instance = this.instance;
						if (instance == null) {
							this.instance = instance = new NamedThreadFactory(namePrefix);
						}
					}
				}
				return instance.newThread(r);
			}
		};
	}

}
