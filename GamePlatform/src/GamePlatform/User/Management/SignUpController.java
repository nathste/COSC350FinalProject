package GamePlatform.User.Management;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import GamePlatform.Database.DatabaseService;
import GamePlatform.Utility.LanguageUtil;
import GamePlatform.Utility.EmailUtil;
import java.util.Optional;

public class SignUpController {
    @FXML private Label titleLabel;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField verificationCodeField;
    @FXML private Button sendCodeButton;
    @FXML private Button signUpButton;
    
    private static Map<String, String> verificationCodes = new HashMap<>();
    private static Map<String, Timer> verificationTimers = new HashMap<>();
    
    @FXML
    private void handleSendCode() {
        String email = emailField.getText().trim();
        
        if (!EmailUtil.isValidEmail(email)) {
            showError(
                LanguageUtil.isEnglish() ? "Invalid Email" : "无效的邮箱",
                LanguageUtil.isEnglish() ? "Please enter a valid email address" : "请输入有效的邮箱地址"
            );
            return;
        }
        
        // 发送验证码
        String code = EmailUtil.sendVerificationCode(email);
        if (code != null) {
            // 保存验证码
            verificationCodes.put(email, code);
            
            // 设置10分钟后过期
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    verificationCodes.remove(email);
                }
            }, 10 * 60 * 1000); // 10分钟
            
            // 保存定时器以便取消
            if (verificationTimers.containsKey(email)) {
                verificationTimers.get(email).cancel();
            }
            verificationTimers.put(email, timer);
            
            // 禁用发送按钮60秒
            sendCodeButton.setDisable(true);
            Timer cooldownTimer = new Timer();
            cooldownTimer.scheduleAtFixedRate(new TimerTask() {
                int countdown = 60;
                @Override
                public void run() {
                    if (countdown > 0) {
                        javafx.application.Platform.runLater(() -> 
                            sendCodeButton.setText(countdown + "s")
                        );
                        countdown--;
                    } else {
                        javafx.application.Platform.runLater(() -> {
                            sendCodeButton.setDisable(false);
                            sendCodeButton.setText(LanguageUtil.isEnglish() ? 
                                "Send Code" : "发送验证码");
                        });
                        this.cancel();
                    }
                }
            }, 0, 1000);
            
            showInfo(
                LanguageUtil.isEnglish() ? "Success" : "成功",
                LanguageUtil.isEnglish() ? 
                    "Verification code has been sent to your email" :
                    "验证码已发送到您的邮箱"
            );
        } else {
            showError(
                LanguageUtil.isEnglish() ? "Error" : "错误",
                LanguageUtil.isEnglish() ? 
                    "Failed to send verification code" :
                    "发送验证码失败"
            );
        }
    }
    
    @FXML
    private void handleSignUp() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String verificationCode = verificationCodeField.getText().trim();
        
        // 基本验证
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || verificationCode.isEmpty()) {
            showError(
                LanguageUtil.isEnglish() ? "Error" : "错误",
                LanguageUtil.isEnglish() ? "Please fill in all fields" : "请填写所有字段"
            );
            return;
        }
        
        // 验证密码匹配
        if (!password.equals(confirmPassword)) {
            showError(
                LanguageUtil.isEnglish() ? "Error" : "错误",
                LanguageUtil.isEnglish() ? "Passwords do not match" : "两次输入的密码不匹配"
            );
            return;
        }
        
        // 验证邮箱格式
        if (!EmailUtil.isValidEmail(email)) {
            showError(
                LanguageUtil.isEnglish() ? "Error" : "错误",
                LanguageUtil.isEnglish() ? "Invalid email format" : "邮箱格式无效"
            );
            return;
        }
        
        // 验证验证码
        String savedCode = verificationCodes.get(email);
        if (savedCode == null || !savedCode.equals(verificationCode)) {
            showError(
                LanguageUtil.isEnglish() ? "Error" : "错误",
                LanguageUtil.isEnglish() ? "Invalid or expired verification code" : "验证码无效或已过期"
            );
            return;
        }
        
        // 检查邮箱是否已注册
        if (DatabaseService.isEmailRegistered(email)) {
            // 如果邮箱已注册，提供重置密码和修改用户名的选项
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(LanguageUtil.isEnglish() ? "Email Exists" : "邮箱已存在");
            alert.setHeaderText(null);
            alert.setContentText(LanguageUtil.isEnglish() ? 
                "This email is already registered. Would you like to reset your password or update your username?" :
                "该邮箱已注册。您是要重置密码还是更新用户名？");
            
            ButtonType resetPasswordButton = new ButtonType(
                LanguageUtil.isEnglish() ? "Reset Password" : "重置密码"
            );
            ButtonType updateUsernameButton = new ButtonType(
                LanguageUtil.isEnglish() ? "Update Username" : "更新用户名"
            );
            ButtonType cancelButton = new ButtonType(
                LanguageUtil.isEnglish() ? "Cancel" : "取消", 
                ButtonBar.ButtonData.CANCEL_CLOSE
            );
            
            alert.getButtonTypes().setAll(resetPasswordButton, updateUsernameButton, cancelButton);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == resetPasswordButton) {
                    // 重置密码
                    if (DatabaseService.updateUserPassword(email, password)) {
                        showInfo(
                            LanguageUtil.isEnglish() ? "Success" : "成功",
                            LanguageUtil.isEnglish() ? 
                                "Password has been reset successfully" :
                                "密码重置成功"
                        );
                        ((Stage) signUpButton.getScene().getWindow()).close();
                    } else {
                        showError(
                            LanguageUtil.isEnglish() ? "Error" : "错误",
                            LanguageUtil.isEnglish() ? 
                                "Failed to reset password" :
                                "密码重置失败"
                        );
                    }
                } else if (result.get() == updateUsernameButton) {
                    // 更新用户名
                    if (DatabaseService.updateUsername(email, username)) {
                        showInfo(
                            LanguageUtil.isEnglish() ? "Success" : "成功",
                            LanguageUtil.isEnglish() ? 
                                "Username has been updated successfully" :
                                "用户名更新成功"
                        );
                        ((Stage) signUpButton.getScene().getWindow()).close();
                    } else {
                        showError(
                            LanguageUtil.isEnglish() ? "Error" : "错误",
                            LanguageUtil.isEnglish() ? 
                                "Failed to update username" :
                                "用户名更新失败"
                        );
                    }
                }
            }
            return;
        }
        
        // 新用户注册
        if (DatabaseService.registerUser(username, email, password)) {
            showInfo(
                LanguageUtil.isEnglish() ? "Success" : "成功",
                LanguageUtil.isEnglish() ? 
                    "Registration successful! Please login." :
                    "注册成功！请登录。"
            );
            ((Stage) signUpButton.getScene().getWindow()).close();
        } else {
            showError(
                LanguageUtil.isEnglish() ? "Error" : "错误",
                LanguageUtil.isEnglish() ? "Registration failed" : "注册失败"
            );
        }
    }
    
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 