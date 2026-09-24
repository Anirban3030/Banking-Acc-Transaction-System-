package com.tcs.banking.controller;

import com.tcs.banking.dto.LoginRequest;
import com.tcs.banking.dto.RegisterRequest;
import com.tcs.banking.dto.DashboardData;
import com.tcs.banking.dto.AccountResponse;
import com.tcs.banking.dto.TransactionResponse;
import com.tcs.banking.dto.AmountRequest;
import com.tcs.banking.dto.TransferRequest;
import com.tcs.banking.entity.Customer;
import com.tcs.banking.exception.AccountBlockedException;
import com.tcs.banking.exception.DuplicateEmailException;
import com.tcs.banking.exception.InsufficientBalanceException;
import com.tcs.banking.exception.InvalidTransactionException;
import com.tcs.banking.exception.ResourceNotFoundException;
import com.tcs.banking.service.AuthService;
import com.tcs.banking.service.DashboardService;
import com.tcs.banking.service.AccountService;
import com.tcs.banking.service.TransactionService;
import com.tcs.banking.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final AuthService authService;
    private final DashboardService dashboardService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final CustomerService customerService;

    @GetMapping("/")
    public String home(HttpSession session) {
        return session.getAttribute("customerId") == null
                ? "redirect:/login"
                : "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginRequest loginRequest,
                        BindingResult bindingResult,
                        Model model,
                        HttpSession session) {
        if (bindingResult.hasErrors()) {
            return "login";
        }

        try {
            Customer customer = authService.login(loginRequest);
            session.setAttribute("customerId", customer.getId());
            session.setAttribute("customerName", customer.getName());
            return "redirect:/dashboard";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", "Invalid email or password.");
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest registerRequest,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            authService.register(registerRequest);
            return "redirect:/login?registered";
        } catch (DuplicateEmailException ex) {
            model.addAttribute("error", "This email is already registered.");
            return "register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/login";
        }

        DashboardData dashboard = dashboardService.getDashboard(customerId);
        model.addAttribute("dashboard", dashboard);
        return "dashboard";
    }

    @GetMapping("/accounts")
    public String accounts(HttpSession session, Model model) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/login";
        }

        model.addAttribute("accounts", accountService.getMyAccounts(customerId));
        return "accounts";
    }

    @GetMapping("/accounts/{id}")
    public String accountDetails(@PathVariable Long id, HttpSession session, Model model) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/login";
        }

        AccountResponse account = accountService.getMyAccount(id, customerId);
        List<TransactionResponse> transactions = transactionService.getHistory(id, customerId);
        model.addAttribute("account", account);
        model.addAttribute("transactions", transactions);
        return "account-details";
    }

    @GetMapping("/accounts/{id}/deposit")
    public String depositPage(@PathVariable Long id, HttpSession session, Model model) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/login";
        }

        model.addAttribute("account", accountService.getMyAccount(id, customerId));
        model.addAttribute("amountRequest", new AmountRequest());
        model.addAttribute("operation", "Deposit");
        return "money-operation";
    }

    @PostMapping("/accounts/{id}/deposit")
    public String deposit(@PathVariable Long id,
                          @Valid @ModelAttribute AmountRequest amountRequest,
                          BindingResult bindingResult,
                          HttpSession session,
                          Model model) {
        return processMoneyOperation(id, amountRequest, bindingResult, session, model, "Deposit");
    }

    @GetMapping("/accounts/{id}/withdraw")
    public String withdrawPage(@PathVariable Long id, HttpSession session, Model model) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/login";
        }

        model.addAttribute("account", accountService.getMyAccount(id, customerId));
        model.addAttribute("amountRequest", new AmountRequest());
        model.addAttribute("operation", "Withdraw");
        return "money-operation";
    }

    @PostMapping("/accounts/{id}/withdraw")
    public String withdraw(@PathVariable Long id,
                           @Valid @ModelAttribute AmountRequest amountRequest,
                           BindingResult bindingResult,
                           HttpSession session,
                           Model model) {
        return processMoneyOperation(id, amountRequest, bindingResult, session, model, "Withdraw");
    }

    @GetMapping("/transfer")
    public String transferPage(HttpSession session, Model model) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/login";
        }

        model.addAttribute("accounts", accountService.getMyAccounts(customerId));
        model.addAttribute("transferRequest", new TransferRequest());
        return "transfer";
    }

    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute TransferRequest transferRequest,
                           BindingResult bindingResult,
                           HttpSession session,
                           Model model) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/login";
        }

        model.addAttribute("accounts", accountService.getMyAccounts(customerId));
        model.addAttribute("transferRequest", transferRequest);

        if (bindingResult.hasErrors()) {
            return "transfer";
        }

        try {
            transactionService.transfer(customerId, transferRequest);
            return "redirect:/transfer?success";
        } catch (InsufficientBalanceException | AccountBlockedException
                 | InvalidTransactionException | ResourceNotFoundException ex) {
            model.addAttribute("error", ex.getMessage());
            return "transfer";
        }
    }

    @GetMapping("/transactions")
    public String transactions(HttpSession session, Model model) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/login";
        }

        model.addAttribute("accounts", accountService.getMyAccounts(customerId));
        model.addAttribute("selectedAccountId", null);
        model.addAttribute("transactions", List.of());
        return "transactions";
    }

    @GetMapping("/transactions/{accountId}")
    public String transactionsForAccount(@PathVariable Long accountId,
                                         HttpSession session,
                                         Model model) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/login";
        }

        model.addAttribute("accounts", accountService.getMyAccounts(customerId));
        model.addAttribute("selectedAccountId", accountId);
        model.addAttribute("selectedAccount", accountService.getMyAccount(accountId, customerId));
        model.addAttribute("transactions", transactionService.getHistory(accountId, customerId));
        return "transactions";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/login";
        }

        model.addAttribute("customer", customerService.getProfile(customerId));
        return "profile";
    }

    private String processMoneyOperation(Long accountId,
                                         AmountRequest amountRequest,
                                         BindingResult bindingResult,
                                         HttpSession session,
                                         Model model,
                                         String operation) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/login";
        }

        AccountResponse account = accountService.getMyAccount(accountId, customerId);
        model.addAttribute("account", account);
        model.addAttribute("amountRequest", amountRequest);
        model.addAttribute("operation", operation);

        if (bindingResult.hasErrors()) {
            return "money-operation";
        }

        try {
            if ("Deposit".equals(operation)) {
                accountService.deposit(accountId, customerId, amountRequest);
            } else {
                accountService.withdraw(accountId, customerId, amountRequest);
            }
            return "redirect:/accounts/" + accountId + "?updated";
        } catch (InsufficientBalanceException | AccountBlockedException ex) {
            model.addAttribute("error", ex.getMessage());
            return "money-operation";
        }
    }
}
