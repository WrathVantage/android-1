/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.android.designer.propertyTable.editors;

import com.intellij.designer.model.RadComponent;
import com.intellij.designer.propertyTable.editors.ComboEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Alexander Lobas
 */
public class StringsComboEditor extends ComboEditor {
  public StringsComboEditor(String[] values) {
    myCombo.setModel(new DefaultComboBoxModel(values));
  }

  @NotNull
  @Override
  public JComponent getComponent(@NotNull RadComponent rootComponent, @Nullable RadComponent component, Object value) {
    myCombo.setSelectedItem(value);
    return myCombo;
  }

  @Override
  public Object getValue() throws Exception {
    return myCombo.getSelectedItem();
  }
}