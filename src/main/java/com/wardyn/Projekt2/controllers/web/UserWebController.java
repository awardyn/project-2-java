package com.wardyn.Projekt2.controllers.web;

import com.wardyn.Projekt2.domains.App;
import com.wardyn.Projekt2.domains.Search;
import com.wardyn.Projekt2.domains.User;
import com.wardyn.Projekt2.enums.Role;
import com.wardyn.Projekt2.services.interfaces.AppService;
import com.wardyn.Projekt2.services.interfaces.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class UserWebController {
    private final UserService userService;
    private final AppService appService;

    public UserWebController(UserService userService, AppService appService) {
        this.userService = userService;
        this.appService = appService;
    }

    @GetMapping("/users")
    public String getUsers(Model model, @CookieValue(value = "id", defaultValue = "-1") String id) {
        Long parsedId = Long.parseLong(id);

        Optional<User> loggedUser = userService.getUserById(parsedId);

        if (loggedUser.isPresent()) {
            User user = loggedUser.get();
            boolean isAdmin = user.getRole().equals(Role.ADMIN);
            if (isAdmin) {
                List<List<Object>> list = createListOfApps();

                model.addAttribute("apps", list);
                model.addAttribute("search", new Search());
                model.addAttribute("users", userService.getUsers());
                return "user/users";
            }
        }

        return "redirect:/";
    }

    @GetMapping("/users/search")
    public String getUsersByAppName(Search search, Model model, @CookieValue(value = "id", defaultValue = "-1") String id) {
        if(search.getSearchBy().equals(-1L)) {
            return "redirect:/users";
        }

        Long parsedId = Long.parseLong(id);

        Optional<User> loggedUser = userService.getUserById(parsedId);

        if (loggedUser.isPresent()) {
            User user = loggedUser.get();
            boolean isAdmin = user.getRole().equals(Role.ADMIN);
            if (isAdmin) {
                List<List<Object>> list = createListOfApps();
                App appSearch = appService.getAppById(search.getSearchBy()).get();

                model.addAttribute("apps", list);
                model.addAttribute("search", new Search(search.getSearchBy()));
                model.addAttribute("users", userService.getUsersByAppId(appSearch));
                return "user/users";
            }
        }

        return "redirect:/";
    }

    @GetMapping("/users/{id}")
    public String getUserById(@PathVariable Long id, Model model, @CookieValue(value = "id", defaultValue = "-1") String cookieId) {
        Long parsedId = Long.parseLong(cookieId);

        Optional<User> loggedUser = userService.getUserById(parsedId);

        if (loggedUser.isPresent()) {
            User user = loggedUser.get();
            boolean isAdmin = user.getRole().equals(Role.ADMIN);
            if (isAdmin) {
                Optional<User> userToDisplay = userService.getUserById(id);
                if (!userToDisplay.isPresent()) {
                    model.addAttribute("error", "There is no user with given id");
                    return "user/user";
                }

                model.addAttribute("user", userToDisplay.get());
                model.addAttribute("apps", userToDisplay.get().getAppList());

                return "user/user";
            }
        }

        return "redirect:/";
    }

    @GetMapping("/users/create")
    public String userCreate(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("action", "create");

        return "user/userForm";
    }

    @GetMapping("/users/{id}/edit")
    public String userEdit(@PathVariable Long id, Model model, @CookieValue(value = "id", defaultValue = "-1") String cookieId) {
        Long parsedId = Long.parseLong(cookieId);

        Optional<User> loggedUser = userService.getUserById(parsedId);

        if (loggedUser.isPresent()) {
            User user = loggedUser.get();
            boolean isAdmin = user.getRole().equals(Role.ADMIN);
            if (isAdmin || id.equals(parsedId)) {
                Optional<User> userToEdit = userService.getUserById(id);
                if (!userToEdit.isPresent()) {
                    model.addAttribute("error", "There is no user with given id");
                    return "user/userForm";
                }
                model.addAttribute("user", userToEdit.get());
                model.addAttribute("action", "edit");

                return "user/userForm";
            }
        }
        return "redirect:/";
    }

    @PostMapping("/users/create")
    public String createUser(@Valid User user, BindingResult errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("action", "create");
            return "user/userForm";
        }

        List<String> usernames = userService.getUsers().stream().map(User::getUsername).collect(Collectors.toList());

        if (usernames.contains(user.getUsername())) {
            ObjectError error = new ObjectError("domain", "Username is not unique");
            errors.addError(error);
            model.addAttribute("action", "create");
            return "user/userForm";
        }

        user.setRole(Role.USER);
        userService.addUser(user);

        return "redirect:/users";
    }

    @PostMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, @Valid User user, BindingResult errors, RedirectAttributes redirectAttributes, Model model, @CookieValue(value = "id", defaultValue = "-1") String cookieId) {
        if (errors.hasErrors()) {
            model.addAttribute("action", "edit");
            return "user/userForm";
        }

        List<String> usernames = userService.getUsers().stream().filter(u -> !Objects.equals(u.getId(), user.getId())).map(User::getUsername).collect(Collectors.toList());

        if (usernames.contains(user.getUsername())) {
            ObjectError error = new ObjectError("domain", "Username is not unique");
            errors.addError(error);
            model.addAttribute("action", "create");
            return "user/userForm";
        }

        Boolean edited = userService.editUser(user);

        if (edited.equals(false)) {
            redirectAttributes.addFlashAttribute("error", "There is no user with given id");
            return "redirect:/users/" + user.getId() + "/edit";
        }

        if (id.equals(Long.parseLong(cookieId))) {
            return "redirect:/";
        }

        return "redirect:/users/" + user.getId();
    }

    @DeleteMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpServletResponse response, @CookieValue(value = "id", defaultValue = "-1") String cookieId) {
        Boolean deleted = userService.deleteUser(id);

        if (deleted.equals(false)) {
            redirectAttributes.addFlashAttribute("error", "There is no user with given id to delete");
            return "redirect:/users";
        }

        if (id.equals(Long.parseLong(cookieId))) {
            Cookie cookie = new Cookie("id", "-1");
            response.addCookie(cookie);
            return "redirect:/";
        }

        return "redirect:/users";
    }

    private List<List<Object>> createListOfApps() {
        List<List<Object>> list = new ArrayList<>();
        List<Object> all = new ArrayList<>();
        all.add(-1L);
        all.add("all");

        list.add(all);

        List<App> appList = appService.getApps();

        for (App app : appList) {
            List<Object> element = new ArrayList<>();
            element.add(app.getId());
            element.add(app.getName());
            list.add(element);
        }

        return list;
    }
}
