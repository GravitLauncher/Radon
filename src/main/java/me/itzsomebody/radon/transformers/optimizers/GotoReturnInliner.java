/*
 * Copyright (C) 2018 ItzSomebody
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package me.itzsomebody.radon.transformers.optimizers;

import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.itzsomebody.radon.Logger;
import me.itzsomebody.radon.utils.BytecodeUtils;

/**
 * Inlines goto-return sequences by setting the goto to a return opcode.
 *
 * @author ItzSomebody.
 */
public class GotoReturnInliner extends Optimizer {
    @Override
    public void transform() {
        AtomicInteger count = new AtomicInteger();
        long current = System.currentTimeMillis();

        getClassWrappers().stream().filter(classWrapper -> !excluded(classWrapper)).forEach(classWrapper ->
                classWrapper.methods.stream().filter(methodWrapper -> !excluded(methodWrapper)
                        && hasInstructions(methodWrapper.methodNode)).forEach(methodWrapper -> {
                    MethodNode methodNode = methodWrapper.methodNode;

                    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
                        if (insn.getOpcode() == GOTO) {
                            JumpInsnNode gotoJump = (JumpInsnNode) insn;
                            AbstractInsnNode insnAfterTarget = gotoJump.label.getNext();
                            if (insnAfterTarget != null && BytecodeUtils.isReturn(insnAfterTarget.getOpcode())) {
                                methodNode.instructions.set(insn, new InsnNode(insnAfterTarget.getOpcode()));
                                count.incrementAndGet();
                            }
                        }
                    }
                }));

        Logger.stdOut(String.format("Inlined %d GOTO->RETURN sequences. [%dms]", count.get(),
                tookThisLong(current)));
    }

    @Override
    public String getName() {
        return "GOTO->Return Remover";
    }
}
