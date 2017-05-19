package com.zxj.plugin.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.intellij.diagnostic.IdeErrorsDialog;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.vfs.VcsVirtualFolder;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.vfs.local.CoreLocalVirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl;
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileImpl;
import com.zxj.plugin.component.MybatisProjectComponent;
import com.zxj.plugin.config.CRUDDialogConfig;
import com.zxj.plugin.util.*;
import com.zxj.plugin.view.CRUDDialog;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.tree.BaseElement;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * Created by zhuxiujie
 */
public class MybatisResetCURDAction extends AnAction {


    @Override
    public void update(AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(true);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        // Does nothing
        VirtualFile eventFile = FileUtil.getFile(event.getDataContext());
        String extension = eventFile.getExtension();

        if (extension != null && extension.equals("xml")) {
            CRUDDialog crudDialog = new CRUDDialog(new CRUDDialog.Resulet() {
                @Override
                public void result(CRUDDialogConfig crudDialogConfig) {
                    if (crudDialogConfig.getTableName() == null || crudDialogConfig.getTableName().equals("")) {
                        Messages.showMessageDialog(
                                "表名不可为空！",
                                "error",
                                Messages.getErrorIcon()
                        );
                    } else {
                        readData(eventFile, event, crudDialogConfig);
                    }
                }
            });
            crudDialog.pack();
            crudDialog.setVisible(true);
        } else {
            Messages.showMessageDialog(
                    eventFile.getName() + " is not a .xml file!",

                    "error",

                    Messages.getErrorIcon()

            );
        }
    }

    private void readData(VirtualFile file, AnActionEvent event, CRUDDialogConfig crudDialogConfig) {
        ReaderXML.read(file.getPath(), new ReaderXML.XMLInterface() {
            @Override
            public void update(Document document) {
                try {
                    runConfigure(document, event, crudDialogConfig);
                    ReaderXML.writer(document, file.getParent().getPath() + "/" + file.getName());
                    ProjectUtil.invate();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }


    private void runConfigure(Document document, AnActionEvent e, CRUDDialogConfig crudDialogConfig) throws Exception {
        String baseDir = e.getProject().getBaseDir().getPath();
        String fileName = baseDir + "/mybitsUpdateConfig.properties";
        Properties properties = new Properties();
        PropertyUtil.read(properties, fileName);

        Element rootElement = document.getRootElement();
        List<Element> elementList = rootElement.elements();
        for (Element element : elementList) {// <result <select
            if (checkNeedRemove(element)) {// remove
                // delete
                element.getParent().remove(element);
                System.out.println("delete element:" + element.attribute("id").getValue());
            }
        }
        // add select
        if (crudDialogConfig.isSelect()) addSelect(rootElement, crudDialogConfig);
        if (crudDialogConfig.isDelete()) addDelete(rootElement, crudDialogConfig);
        if (crudDialogConfig.isUpdate()) addUpdate(rootElement, crudDialogConfig);
        if (crudDialogConfig.isCreate()) addInsert(rootElement, crudDialogConfig);
        rootElement.addText("\n");
        System.out.println(document.toString());

        System.out.println("success ,");
    }

    private boolean checkNeedRemove(Element element) {
        //if(!attributeStr.equals("BaseResultMap"))return true;
        Attribute attribute = element.attribute("id");
        String attributeStr = attribute.getValue();
        if (attributeStr.equals("selectById")
                || attributeStr.equals("deleteById")
                || attributeStr.equals("updateById")
                || attributeStr.equals("insert")
                || attributeStr.equals("selectByPrimaryKey")
                || attributeStr.equals("deleteByPrimaryKey")
                || attributeStr.equals("insertSelective")
                || attributeStr.equals("updateByPrimaryKeySelective")
                || attributeStr.equals("updateByPrimaryKey")
                ) {
            return true;
        }
        return false;
    }

    private void addSelect(Element rootElement, CRUDDialogConfig crudDialogConfig) {
        String type = rootElement.element("resultMap").element("id").attribute("jdbcType").getValue();

        Element select = new BaseElement("select");
        select.addAttribute("id", "selectById");
        select.addAttribute("parameterType", JDBC2JAVA.getJAVAValue(type));
        select.addAttribute("resultMap", "BaseResultMap");
        String logicDeleteCode = null;
        if (crudDialogConfig.isDeleteFlag()) {
            logicDeleteCode = "     and " + crudDialogConfig.getDeleteFlagStr() + " = " + crudDialogConfig.getUnDeletedStr() + "\n ";
        }
        select.setText("\n     select * from " + crudDialogConfig.getTableName() + "\n\t\twhere id = #{id,jdbcType=INTEGER}\n" + logicDeleteCode);
        rootElement.add(select);
    }

    private void addDelete(Element rootElement, CRUDDialogConfig crudDialogConfig) {
        String type = rootElement.element("resultMap").element("id").attribute("jdbcType").getValue();

        Element select = new BaseElement("update");
        select.addAttribute("id", "deleteById");
        select.addAttribute("parameterType", JDBC2JAVA.getJAVAValue(type));
        //select.addAttribute("resultMap", "BaseResultMap");
        String logicDeleteCode = null;
        if (crudDialogConfig.isDeleteFlag()) {
            logicDeleteCode = "\n\t\tset " + crudDialogConfig.getDeleteFlagStr() + " = " + crudDialogConfig.getDeletedStr();
        }
        select.setText("\n     update " + crudDialogConfig.getTableName() + logicDeleteCode + "\n\t\twhere id = #{id,jdbcType=INTEGER} \n ");
        rootElement.add(select);
    }

    private void addUpdate(Element rootElement, CRUDDialogConfig crudDialogConfig) {
        String type = rootElement.element("resultMap").element("id").attribute("jdbcType").getValue();
        List<Element> attributes = rootElement.element("resultMap").elements("result");

        Element update = new BaseElement("update");
        update.addAttribute("id", "updateById");
        update.addAttribute("parameterType", JDBC2JAVA.getJAVAValue(type));
        //update.addAttribute("resultMap", "BaseResultMap");

        Element set = new BaseElement("set");
        for (Element element : attributes) {
            Element _if = new BaseElement("if");
            String column = element.attributeValue("column");
            String jdbcType = element.attributeValue("jdbcType");
            String property = element.attributeValue("property");
            _if.addAttribute("test", property + " != null");
            _if.setText(column + " = #{" + property + ",jdbcType=" + jdbcType + "},");
            set.add(_if);
        }
        String sets = set.asXML().toString();
        sets = sets.replaceAll("<if", "\n        <if");
        sets = sets.replaceAll("<set>", "\n   <set>");
        sets = sets.replaceAll("</set>", "\n   </set>");

        String logicDeleteCode = null;
        if (crudDialogConfig.isDeleteFlag()) {
            logicDeleteCode = " and " + crudDialogConfig.getDeleteFlagStr() + " = " + crudDialogConfig.getUnDeletedStr() + "\n ";
        }
        update.setText(
                " \n     update " + crudDialogConfig.getTableName() + " " + sets + "\t\twhere id = #{id,jdbcType=INTEGER}" + logicDeleteCode);
        rootElement.add(update);
    }

    private void addInsert(Element rootElement, CRUDDialogConfig crudDialogConfig) {
        String type = rootElement.element("resultMap").attribute("type").getValue();
        List<Element> attributes = rootElement.element("resultMap").elements("result");

        Element insert = new BaseElement("insert");
        insert.addAttribute("id", "insert");
        insert.addAttribute("useGeneratedKeys", "true");
        insert.addAttribute("keyProperty", "id");
        insert.addAttribute("keyColumn", "id");
        insert.addAttribute("parameterType", type);

        // trim1

        Element trim1 = new BaseElement("trim");
        trim1.addAttribute("prefix", "(");
        trim1.addAttribute("suffix", ")");
        trim1.addAttribute("suffixOverrides", ",");
        for (Element element : attributes) {
            Element _if = new BaseElement("if");
            String property = element.attributeValue("property");
            String column = element.attributeValue("column");
            _if.addAttribute("test", property + " != null");
            _if.setText(column + ",");
            trim1.add(_if);
        }
        String trims1 = trim1.asXML().toString();
        trims1 = trims1.replaceAll("<if", "\n        <if");
        trims1 = trims1.replaceAll("<trim>", "\n   <trim>\n");
        trims1 = trims1.replaceAll("</trim>", "\n   </trim>\n");

        // trim 2
        Element trim2 = new BaseElement("trim");
        trim2.addAttribute("prefix", "values (");
        trim2.addAttribute("suffix", ")");
        trim2.addAttribute("suffixOverrides", ",");
        for (Element element : attributes) {
            Element _if = new BaseElement("if");
            String column = element.attributeValue("column");
            String jdbcType = element.attributeValue("jdbcType");
            String property = element.attributeValue("property");
            _if.addAttribute("test", property + " != null");
            _if.setText("#{" + property + ",jdbcType=" + jdbcType + "},");
            trim2.add(_if);
        }
        String trims2 = trim2.asXML().toString();
        trims2 = trims2.replaceAll("<if", "\n        <if");
        trims2 = trims2.replaceAll("<trim>", "\n   <trim>\n");
        trims2 = trims2.replaceAll("</trim>", "\n   </trim>\n");

        insert.setText("\n     insert into " + crudDialogConfig.getTableName() + "\n " + trims1 + "\n" + trims2);
        rootElement.add(insert);

    }
}