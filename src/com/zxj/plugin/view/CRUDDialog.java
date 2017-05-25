package com.zxj.plugin.view;

import com.zxj.plugin.config.CRUDDialogConfig;

import javax.swing.*;
import java.awt.event.*;

public class CRUDDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox select_mothedCheckBox;
    private JCheckBox delete_mothedCheckBox;
    private JCheckBox update_mothedCheckBox;
    private JCheckBox create_return_idCheckBox;
    private JTextField textField1;
    private JCheckBox deleteFlagCheckBox;
    private JTextField delete_flagTextField;
    private JTextField unDeleteTextField;
    private JTextField deletedTextField;
    private JCheckBox selectByConditionCheckBox;
    private JCheckBox countByConditionCheckBox;
    private JTextField selectByTextField;
    private JTextField updateByTextField;
    private JTextField deleteByTextField;
    private JProgressBar progressBar;
    private JLabel progressLabel;


    public interface Resulet{
        void result(CRUDDialogConfig crudDialogConfig);
    }


    Resulet resulet;

    public CRUDDialog(Resulet resulet) {
        this.resulet=resulet;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK(e);
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK(ActionEvent event) {
        // add your code here
        //dispose();
        CRUDDialogConfig crudDialogConfig=new CRUDDialogConfig(textField1.getText(),select_mothedCheckBox.isSelected(),delete_mothedCheckBox.isSelected(),update_mothedCheckBox.isSelected(),create_return_idCheckBox.isSelected(),deleteFlagCheckBox.isSelected(),delete_flagTextField.getText(),deletedTextField.getText(),unDeleteTextField.getText());
        crudDialogConfig.setCountByCondition(countByConditionCheckBox.isSelected());
        crudDialogConfig.setSelectByCondition(selectByConditionCheckBox.isSelected());

        crudDialogConfig.setSelectByTextField(selectByTextField.getText().toString());
        crudDialogConfig.setUpdateByTextField(updateByTextField.getText().toString());
        crudDialogConfig.setDeleteByTextField(deleteByTextField.getText().toString());

        resulet.result(crudDialogConfig);
    }

    public void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
        CRUDDialog dialog = new CRUDDialog(new Resulet() {
            @Override
            public void result(CRUDDialogConfig crudDialogConfig) {

                System.out.println();
            }
        });
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    public JTextField getSelectByTextField() {
        return selectByTextField;
    }

    public JTextField getUpdateByTextField() {
        return updateByTextField;
    }

    public JTextField getDeleteByTextField() {
        return deleteByTextField;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public JLabel getProgressLabel() {
        return progressLabel;
    }
}
