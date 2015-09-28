/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.editors.gfxtrace.controllers;

import com.android.tools.idea.editors.gfxtrace.GfxTraceEditor;
import com.android.tools.idea.editors.gfxtrace.service.path.Path;
import com.intellij.execution.ui.layout.impl.JBRunnerTabs;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.ui.ThreeComponentsSplitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainController extends Controller {
  public static JComponent createUI(GfxTraceEditor editor) {
    return new MainController(editor).myPanel;
  }

  @NotNull private JBPanel myPanel = new JBPanel(new BorderLayout());

  private MainController(@NotNull GfxTraceEditor editor) {
    super(editor);
    ThreeComponentsSplitter threePanes = new ThreeComponentsSplitter(true);
    myPanel.add(threePanes, BorderLayout.CENTER);
    threePanes.setDividerWidth(5);

    ContextController.loadReplayDevice(editor);

    // Add the scrubber view to the top panel.
    threePanes.setFirstComponent(ScrubberController.createUI(editor));
    threePanes.setFirstSize(150);

    // Now add the atom tree and buffer views to the middle pane in the main pane.
    final JBSplitter middleSplitter = new JBSplitter(false);
    middleSplitter.setMinimumSize(JBUI.size(100, 10));
    middleSplitter.setFirstComponent(AtomController.createUI(editor));
    middleSplitter.setSecondComponent(FrameBufferController.createUI(editor));
    middleSplitter.setProportion(0.3f);
    threePanes.setInnerComponent(middleSplitter);

    // Configure the miscellaneous tabs.
    JBRunnerTabs miscTabs = new JBRunnerTabs(editor.getProject(), ActionManager.getInstance(), IdeFocusManager.findInstance(), this);
    miscTabs.setPaintBorder(0, 0, 0, 0).setTabSidePaintBorder(1).setPaintFocus(UIUtil.isUnderDarcula() || UIUtil.isUnderIntelliJLaF())
      .setAlwaysPaintSelectedTab(UIUtil.isUnderDarcula() || UIUtil.isUnderIntelliJLaF());
    miscTabs.setBorder(JBUI.Borders.empty(0, 2, 0, 0));

    // Add the textures view to the misc tabs.
    miscTabs.addTab(new TabInfo(TexturesController.createUI(editor)).setText("Textures"));
    miscTabs.addTab(new TabInfo(MemoryController.createUI(editor)).setText("Memory"));
    miscTabs.addTab(new TabInfo(DocumentationController.createUI(editor)).setText("Docs"));

    // More borders for miscellaneous tabs.
    JPanel miscPanel = new JPanel(new BorderLayout());
    miscPanel.setBorder(BorderFactory.createLineBorder(JBColor.border()));
    miscPanel.add(miscTabs, BorderLayout.CENTER);

    // Configure the bottom splitter.
    JBSplitter bottomSplitter = new JBSplitter(false);
    bottomSplitter.setMinimumSize(JBUI.size(100, 10));
    bottomSplitter.setFirstComponent(StateController.createUI(editor));
    bottomSplitter.setSecondComponent(miscPanel);
    threePanes.setLastComponent(bottomSplitter);
    threePanes.setLastSize(300);

    // Make sure the bottom splitter honors minimum sizes.
    threePanes.setHonorComponentsMinimumSize(true);
    Disposer.register(this, threePanes);
  }

  @Override
  public void notifyPath(PathEvent event) {
  }

  @Override
  public void clear() {
    myPanel.removeAll();
  }
}