package com.nexaerp.config;

import com.nexaerp.permission.Permission;
import com.nexaerp.permission.PermissionRepository;
import com.nexaerp.role.Role;
import com.nexaerp.role.RoleRepository;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import com.nexaerp.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.email}")
    private String adminEmail;

    @Value("${app.default-admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        seedPermissions();
        seedRoles();
        seedAdminUser();
        // Default account mappings (DEFAULT_RECEIVABLE_ACCOUNT etc.) are no
        // longer auto-seeded here - they depend on the company's own Chart
        // of Accounts, which doesn't exist yet on a fresh install. They must
        // be configured once from Settings -> Default Accounts; until then,
        // any feature that needs one throws a clear "not configured" error.
    }


    // -----Assign Permission-----


    private void seedPermissions() {

        List<Object[]> permissions = List.of(
                // code, name, module
                new Object[]{"VIEW_ACCOUNTS", "View Accounts", "ACCOUNT"},
                new Object[]{"CREATE_ACCOUNT", "Create Account", "ACCOUNT"},
                new Object[]{"EDIT_ACCOUNT", "Edit Account", "ACCOUNT"},
                new Object[]{"DEACTIVATE_ACCOUNT", "Deactivate Account", "ACCOUNT"},
                new Object[]{"LOOKUP_ACCOUNTS", "Lookup Accounts", "ACCOUNT"},

                new Object[]{"VIEW_JOURNAL", "View Journal", "JOURNAL"},
                new Object[]{"CREATE_JOURNAL", "Create Journal", "JOURNAL"},
                new Object[]{"POST_JOURNAL", "Post Journal", "JOURNAL"},
                new Object[]{"REVERSE_JOURNAL", "Reverse Journal", "JOURNAL"},
                new Object[]{"DELETE_JOURNAL", "Delete Journal", "JOURNAL"},

                //credit note
                new Object[]{"VIEW_CREDIT_NOTE", "View Credit Note", "CREDIT_NOTE"},
                new Object[]{"CREATE_CREDIT_NOTE", "Create Credit Note", "CREDIT_NOTE"},
                new Object[]{"EDIT_CREDIT_NOTE", "Edit Credit Note", "CREDIT_NOTE"},
                new Object[]{"APPROVE_CREDIT_NOTE", "Approve Credit Note", "CREDIT_NOTE"},
                new Object[]{"POST_CREDIT_NOTE", "Post Credit Note", "CREDIT_NOTE"},
                new Object[]{"CANCEL_CREDIT_NOTE", "Cancel Credit Note", "CREDIT_NOTE"},
                new Object[]{"DELETE_CREDIT_NOTE", "Delete Credit Note", "CREDIT_NOTE"},

                // debit note
                new Object[]{"VIEW_DEBIT_NOTE", "View Debit Note", "DEBIT_NOTE"},
                new Object[]{"CREATE_DEBIT_NOTE", "Create Debit Note", "DEBIT_NOTE"},
                new Object[]{"EDIT_DEBIT_NOTE", "Edit Debit Note", "DEBIT_NOTE"},
                new Object[]{"APPROVE_DEBIT_NOTE", "Approve Debit Note", "DEBIT_NOTE"},
                new Object[]{"POST_DEBIT_NOTE", "Post Debit Note", "DEBIT_NOTE"},
                new Object[]{"CANCEL_DEBIT_NOTE", "Cancel Debit Note", "DEBIT_NOTE"},
                new Object[]{"DELETE_DEBIT_NOTE", "Delete Debit Note", "DEBIT_NOTE"},


                new Object[]{"VIEW_PARTY", "View Party", "PARTY"},
                new Object[]{"CREATE_PARTY", "Create Party", "PARTY"},
                new Object[]{"EDIT_PARTY", "Edit Party", "PARTY"},
                new Object[]{"DEACTIVATE_PARTY", "Deactivate Party", "PARTY"},
                new Object[]{"LOOKUP_PARTIES", "Lookup Parties", "PARTY"},

                new Object[]{"VIEW_INVOICE", "View Invoice", "INVOICE"},
                new Object[]{"CREATE_INVOICE", "Create Invoice", "INVOICE"},
                new Object[]{"EDIT_INVOICE", "Edit Invoice", "INVOICE"},
                new Object[]{"POST_INVOICE", "Post Invoice", "INVOICE"},
                new Object[]{"CANCEL_INVOICE", "Cancel Invoice", "INVOICE"},

                new Object[]{"VIEW_VENDOR_BILL", "View Vendor Bill", "VENDOR_BILL"},
                new Object[]{"CREATE_VENDOR_BILL", "Create Vendor Bill", "VENDOR_BILL"},
                new Object[]{"EDIT_VENDOR_BILL", "Edit Vendor Bill", "VENDOR_BILL"},
                new Object[]{"APPROVE_VENDOR_BILL", "Approve Vendor Bill", "VENDOR_BILL"},
                new Object[]{"POST_VENDOR_BILL", "Post Vendor Bill", "VENDOR_BILL"},
                new Object[]{"CANCEL_VENDOR_BILL", "Cancel Vendor Bill", "VENDOR_BILL"},

                // expense
                new Object[]{"VIEW_EXPENSE", "View Expense", "EXPENSE"},
                new Object[]{"CREATE_EXPENSE", "Create Expense", "EXPENSE"},
                new Object[]{"CANCEL_EXPENSE", "Cancel Expense", "EXPENSE"},

                // recurring expense
                new Object[]{"VIEW_RECURRING_EXPENSE", "View Recurring Expense", "RECURRING_EXPENSE"},
                new Object[]{"CREATE_RECURRING_EXPENSE", "Create Recurring Expense", "RECURRING_EXPENSE"},
                new Object[]{"EDIT_RECURRING_EXPENSE", "Edit Recurring Expense", "RECURRING_EXPENSE"},

                // budget
                new Object[]{"VIEW_BUDGET", "View Budget", "BUDGET"},
                new Object[]{"CREATE_BUDGET", "Create Budget", "BUDGET"},
                new Object[]{"EDIT_BUDGET", "Edit Budget", "BUDGET"},
                new Object[]{"DELETE_BUDGET", "Delete Budget", "BUDGET"},
                new Object[]{"ACTIVATE_BUDGET", "Activate Budget", "BUDGET"},
                new Object[]{"CLOSE_BUDGET", "Close Budget", "BUDGET"},
                new Object[]{"VIEW_BUDGET_REPORT", "View Budget Variance Report", "BUDGET"},

                new Object[]{"VIEW_PAYMENT", "View Payment", "PAYMENT"},
                new Object[]{"CREATE_PAYMENT", "Create Payment", "PAYMENT"},
                new Object[]{"POST_PAYMENT", "Post Payment", "PAYMENT"},
                new Object[]{"CANCEL_PAYMENT", "Cancel Payment", "PAYMENT"},

                new Object[]{"VIEW_LEDGER", "View Ledger", "REPORT"},
                new Object[]{"VIEW_TRIAL_BALANCE", "View Trial Balance", "REPORT"},
                new Object[]{"VIEW_REPORT", "View Reports", "REPORT"},

                new Object[]{"VIEW_BANKING", "View Banking", "BANKING"},
                new Object[]{"CREATE_BANKING", "Create Banking", "BANKING"},
                new Object[]{"EDIT_BANKING", "Edit Banking", "BANKING"},

                new Object[]{"VIEW_FIXED_ASSET", "View Fixed Asset", "FIXED_ASSET"},
                new Object[]{"CREATE_FIXED_ASSET", "Create Fixed Asset", "FIXED_ASSET"},
                new Object[]{"EDIT_FIXED_ASSET", "Edit Fixed Asset", "FIXED_ASSET"},
                new Object[]{"RUN_DEPRECIATION", "Run Depreciation", "FIXED_ASSET"},
                new Object[]{"DISPOSE_FIXED_ASSET", "Dispose Fixed Asset", "FIXED_ASSET"},

                new Object[]{"MANAGE_USERS", "Manage Users", "USER_MANAGEMENT"},
                new Object[]{"MANAGE_ROLES", "Manage Roles", "USER_MANAGEMENT"},
                new Object[]{"MANAGE_PERMISSIONS", "Manage Permissions", "USER_MANAGEMENT"},

                new Object[]{"VIEW_AUDIT_LOGS", "View Audit Logs", "AUDIT"},

                new Object[]{"MANAGE_SETTINGS", "Manage System Settings", "SETTINGS"},

                // Fiscal_Year
                new Object[]{"VIEW_FISCAL_YEAR", "View Fiscal Year", "FISCAL_YEAR"},
                new Object[]{"CREATE_FISCAL_YEAR", "Create Fiscal Year", "FISCAL_YEAR"},
                new Object[]{"EDIT_FISCAL_YEAR", "Edit Fiscal Year", "FISCAL_YEAR"},
                new Object[]{"ACTIVATE_FISCAL_YEAR", "Activate Fiscal Year", "FISCAL_YEAR"},
                new Object[]{"CLOSE_FISCAL_YEAR", "Close Fiscal Year", "FISCAL_YEAR"},
                new Object[]{"DELETE_FISCAL_YEAR", "Delete Fiscal Year", "FISCAL_YEAR",},

                //ACCOUNTING_PERIOD
                new Object[]{"VIEW_ACCOUNTING_PERIOD", "View Accounting Period", "ACCOUNTING_PERIOD"},
                new Object[]{"CREATE_ACCOUNTING_PERIOD", "Create Accounting Period", "ACCOUNTING_PERIOD"},
                new Object[]{"EDIT_ACCOUNTING_PERIOD", "Edit Accounting Period", "ACCOUNTING_PERIOD"},
                new Object[]{"OPEN_ACCOUNTING_PERIOD", "Open Accounting Period", "ACCOUNTING_PERIOD"},
                new Object[]{"CLOSE_ACCOUNTING_PERIOD", "Close Accounting Period", "ACCOUNTING_PERIOD"},
                new Object[]{"DELETE_ACCOUNTING_PERIOD", "Delete Accounting Period", "ACCOUNTING_PERIOD"},
                new Object[]{"LOCK_ACCOUNTING_PERIOD", "Lock Accounting Period", "ACCOUNTING_PERIOD"}
                );

        for (Object[] p : permissions) {
            String code = (String) p[0];
            // Skip if  exists
            if (!permissionRepository.existsByCode(code)) {
                permissionRepository.save(Permission.builder()
                        .code(code)
                        .name((String) p[1])
                        .module((String) p[2])
                        .build());
            }
        }
    }


    // -----------Appoint Default Roles with permission

    private void seedRoles() {

        syncSuperAdminRole();

        createRoleIfNotExists(
                "ACCOUNTANT",
                "Accountant",
                permissionRepository.findAll().stream()
                        .filter(permission ->
                                !"USER_MANAGEMENT".equals(permission.getModule())
                        )
                        .toList()
        );

        createRoleIfNotExists(
                "SALES_MANAGER",
                "Sales Manager",
                permissionRepository.findByModule("PARTY").stream()
                        .filter(permission ->
                                List.of(
                                        "VIEW_PARTY",
                                        "CREATE_PARTY",
                                        "EDIT_PARTY"
                                ).contains(permission.getCode())
                        )
                        .toList()
        );

        createRoleIfNotExists(
                "PURCHASE_MANAGER",
                "Purchase Manager",
                permissionRepository.findByModule("VENDOR_BILL")
        );

        createRoleIfNotExists(
                "VIEWER",
                "Viewer",
                permissionRepository.findAll().stream()
                        .filter(permission ->
                                permission.getCode().startsWith("VIEW_")
                                        && !"AUDIT".equals(permission.getModule())
                        )
                        .toList()
        );
    }

    private void createRoleIfNotExists(String name, String description,
                                       List<Permission> permissions) {
        if (!roleRepository.existsByName(name)) {
            roleRepository.save(Role.builder()
                    .name(name)
                    .description(description)
                    .permissions(new HashSet<>(permissions))
                    .build());
        }
    }


    //--- ASSIGN DEFAULT SUPER_ADMIN USER
    private void seedAdminUser() {

        if (!userRepository.existsByEmail(adminEmail)) {

            Role superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                    .orElseThrow(() -> new RuntimeException("SUPER_ADMIN role not found"));

            Set<Role> roles = new HashSet<>();
            roles.add(superAdminRole);

            userRepository.save(User.builder()
                    .name("Super Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .status(UserStatus.ACTIVE)
                    .failedLoginAttempts(0)
                    .roles(roles)
                    .build());
        }
    }
    // all role add+update
    private void syncSuperAdminRole() {
        Role role = roleRepository.findByName("SUPER_ADMIN")
                .orElseGet(() -> Role.builder()
                        .name("SUPER_ADMIN")
                        .description("Super Administrator")
                        .permissions(new HashSet<>())
                        .build()
                );

        if (role.getPermissions() == null) {
            role.setPermissions(new HashSet<>());
        }

        role.getPermissions().addAll(permissionRepository.findAll());

        roleRepository.save(role);
    }

}