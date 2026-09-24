package com.tcs.banking;

import com.tcs.banking.entity.Account;
import com.tcs.banking.repository.AccountRepository;
import com.tcs.banking.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
class BankingUserJourneyTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void customerCanCompleteTheMainUiJourney() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        String email = "journey-" + System.nanoTime() + "@example.com";

        mockMvc.perform(post("/register")
                        .param("name", "Journey User")
                        .param("email", email)
                        .param("phone", "9999999999")
                        .param("password", "secret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        MvcResult login = mockMvc.perform(post("/login")
                        .param("email", email)
                        .param("password", "secret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        var session = login.getRequest().getSession(false);
        Long customerId = customerRepository.findByEmail(email).orElseThrow().getId();
        Account account = accountRepository.findByCustomerId(customerId).get(0);

        mockMvc.perform(get("/dashboard").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Your dashboard")));

        mockMvc.perform(get("/accounts").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("My accounts")));

        mockMvc.perform(get("/accounts/" + account.getId())
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(view().name("account-details"));

        mockMvc.perform(get("/accounts/" + account.getId() + "/deposit")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(view().name("money-operation"))
                .andExpect(model().attribute("operation", "Deposit"));

        mockMvc.perform(post("/accounts/" + account.getId() + "/deposit")
                        .session((org.springframework.mock.web.MockHttpSession) session)
                        .param("amount", "100.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/" + account.getId() + "?updated"));

        mockMvc.perform(post("/accounts/" + account.getId() + "/withdraw")
                        .session((org.springframework.mock.web.MockHttpSession) session)
                        .param("amount", "25.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/" + account.getId() + "?updated"));

        mockMvc.perform(post("/accounts/" + account.getId() + "/withdraw")
                        .session((org.springframework.mock.web.MockHttpSession) session)
                        .param("amount", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("money-operation"))
                .andExpect(model().attributeHasFieldErrors("amountRequest", "amount"));

        mockMvc.perform(get("/transactions/" + account.getId())
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("WITHDRAWAL")));

        mockMvc.perform(get("/profile")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Journey User")));

        mockMvc.perform(get("/logout")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        mockMvc.perform(get("/dashboard")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void protectedPagesRedirectUnauthenticatedUsersToLogin() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(MockMvcRequestBuilders.get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(MockMvcRequestBuilders.get("/accounts"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(MockMvcRequestBuilders.get("/transactions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(MockMvcRequestBuilders.get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
