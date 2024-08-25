package service.vaxapp.controller;

import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import service.vaxapp.MFAService;
import service.vaxapp.UserSession;
import service.vaxapp.model.*;
import service.vaxapp.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AppController {

    private static final Logger logger = LoggerFactory.getLogger(AppController.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ForumAnswerRepository forumAnswerRepository;

    @Autowired
    private ForumQuestionRepository forumQuestionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VaccineCentreRepository vaccineCentreRepository;

    @Autowired
    private VaccineRepository vaccineRepository;

    @Autowired
    private AppointmentSlotRepository appointmentSlotRepository;

    @Autowired
    private UserSession userSession;

    @Autowired
    private UserAnswerFeedbackRepository userAnswerFeedbackRepository;

    @Autowired
    private AppointmentSlotGenerating AppointmentSlotGenerating;

    @Autowired
    private MFAService mfaService;

    @Autowired
    private PasswordEncoder ppsEncoder;

    @Autowired
    private MyUserDetailsService.ValidationService validationService;

    @Autowired
    private SecureYamlHandler yamlHandler;




    @GetMapping("/")
    public String index(Model model) {
        /**
         * Generate slots if they don't exist. Initially the date and time slot was written
         * manually into the database(appointment_slot). This was not feasible for long term. Therefore,
         * the appointmentSlotGenerating.java provide a dynamically class to creates at runtime, new slot
         * for the next 7 days and time slot with 15 mins interval.
         */
        List<VaccineCentre> centres = vaccineCentreRepository.findAll();
        for (VaccineCentre centre : centres) {
            AppointmentSlotGenerating.generateSlotsIfNotExist(centre.getId(), 7, LocalTime.of(9, 0), LocalTime.of(17, 0), 15);
        }

        // Retrieve appointment slots starting from today's date
        ArrayList<AppointmentSlot> appSlots = (ArrayList<AppointmentSlot>) appointmentSlotRepository.findAllByDateAfter(LocalDate.now());

        // sort time slots by center and date
        Collections.sort(appSlots, new Comparator<AppointmentSlot>() {
            public int compare(AppointmentSlot o1, AppointmentSlot o2) {
                if (o1.getVaccineCentre().getName().equals(o2.getVaccineCentre().getName())) {
                    if (o1.getDate().equals(o2.getDate()))
                        return o1.getStartTime().compareTo(o2.getStartTime());
                    return o1.getDate().compareTo(o2.getDate());
                }

                return o1.getVaccineCentre().getName().compareTo(o2.getVaccineCentre().getName());
            }
        });

        // Add the sorted appointment slots and user session to the model
        model.addAttribute("appSlots", appSlots);
        model.addAttribute("userSession", userSession);
        return "index";
    }

    @PostMapping(value = "/make-appointment")
    public String makeAppointment(@RequestParam Map<String, String> body, Model model,
                                  RedirectAttributes redirectAttributes) {
        if (!userSession.isLoggedIn()) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to make an appointment.");
            return "redirect:/login";
        }

        // A user shouldn't have more than one pending appointment
        if (appointmentRepository.findPending(userSession.getUserId()) != null) {
            redirectAttributes.addFlashAttribute("error",
                    "You can only have one pending appointment at a time. Please check your appointment list.");
            return "redirect:/";
        }

        Integer centerId = Integer.valueOf(body.get("center_id"));
        LocalDate date = LocalDate.parse(body.get("date"));
        LocalTime time = LocalTime.parse(body.get("time"));

        AppointmentSlot appSlot = appointmentSlotRepository.findByDetails(centerId, date, time);
        if (appSlot == null) {
            redirectAttributes.addFlashAttribute("error", "The appointment slot you selected is no longer available.");
            return "redirect:/";
        }

        Appointment app = new Appointment(appSlot.getVaccineCentre(), appSlot.getDate(), appSlot.getStartTime(),
                userSession.getUser(), "pending");
        appointmentRepository.save(app);
        appointmentSlotRepository.delete(appSlot);

        redirectAttributes.addFlashAttribute("success",
                "Your appointment has been made! Please see the details of your new appointment.");
        return "redirect:/profile";
    }


    /*******Statistics Area**********/

    @GetMapping("/stats")
    public String statistics(Model model) {
        // Populate the model with statistics for the default country (Irish in this case)
        getStats(model, "Irish");
        return "stats";
    }


    private void getStats(Model model, String country) {
        // Add the user session to the model
        model.addAttribute("userSession", userSession);

        // Add the total number of doses administered to the model
        model.addAttribute("totalDoses", vaccineRepository.count());

        // Retrieve all users_vaccinated who have been vaccinated
        List<User> users_vaccinated = vaccineRepository.findAll().stream().map(Vaccine::getUser).collect(Collectors.toList());
        
        long dosesByNationality = users_vaccinated.stream().distinct().filter(x -> x.getNationality().equalsIgnoreCase(country)).count();
        model.addAttribute("dosesByNationality", dosesByNationality);
        model.addAttribute("country", country);

        // Add the number of doses by nationality to the model
       // model.addAttribute("dosesByNationality",
        //        users_vaccinated.stream().distinct().filter(x -> x.getNationality().equalsIgnoreCase(country)).count());
        //model.addAttribute("country", country);

        // Get unique users based on the vaccinations
        List<User> users_vaccinated_unique = vaccineRepository.findAll().stream()
                .map(Vaccine::getUser)
                .distinct()
                .collect(Collectors.toList());
        // Calculate the total number of users_vaccinated
        long total = users_vaccinated_unique.size();

        // Calculate the number of vaccinated males and females
        long male = users_vaccinated_unique.stream().filter(x -> x.getGender().equalsIgnoreCase("male")).count();
        long female = total - male;

        // Create a map to store age ranges and their corresponding percentages for age chart
        Map<String, Double> ageRanges = new TreeMap<>();
        ageRanges.put("18-25", calculateAgeRangePercentage("18-25", total, users_vaccinated_unique));
        ageRanges.put("26-35", calculateAgeRangePercentage("26-35", total, users_vaccinated_unique));
        ageRanges.put("36-45", calculateAgeRangePercentage("36-45", total, users_vaccinated_unique));
        ageRanges.put("46-55", calculateAgeRangePercentage("46-55", total, users_vaccinated_unique));
        ageRanges.put("56-65", calculateAgeRangePercentage("56-65", total, users_vaccinated_unique));
        ageRanges.put("65+", calculateAgeRangePercentage("65+", total, users_vaccinated_unique));


        // convert to .2 decimcal places for better readability
        DecimalFormat df = new DecimalFormat("#.00");
        double malePercentage = male * 100.0 / (double) total;
        double femalePercentage = female * 100.0 / (double) total;

        // Add the calculated statistics to the model
        //model.addAttribute("agerange", ageRanges);
        //model.addAttribute("maleDosePercent", male * 100.0 / (double) total);
        //model.addAttribute("femaleDosePercent", female * 100.0 / (double) total);

        model.addAttribute("agerange", ageRanges);
        model.addAttribute("maleDosePercent", df.format(malePercentage));
        model.addAttribute("femaleDosePercent", df.format(femalePercentage));

        // Debug: used to verify if the variable are rendering and being passed to stats.html
        logger.info("Total Doses: {}", vaccineRepository.count());
        logger.info("Doses by Nationality: {}", dosesByNationality);
        logger.info("Male Percentage: {}", male * 100.0 / (double) total);
        logger.info("Female Percentage: {}", female * 100.0 / (double) total);
        logger.info("Age Ranges: {}", ageRanges);
        logger.info("Male Percentage: {}", df.format(malePercentage));
        logger.info("Female Percentage: {}", df.format(femalePercentage));

        // Prepare data for charts
        prepareChartData(model, users_vaccinated_unique);
    }

    private void prepareChartData(Model model, List<User> users) {
        // Calculate percentages of males and females
        long total = users.size();
        long male = users.stream().filter(x -> x.getGender().equalsIgnoreCase("male")).count();
        long female = total - male;

        DecimalFormat df = new DecimalFormat("#.00");
        double malePercentage = male * 100.0 / (double) total;
        double femalePercentage = female * 100.0 / (double) total;
        logger.info("Male Percentage: {}", df.format(malePercentage));
        logger.info("Female Percentage: {}", df.format(femalePercentage));

        model.addAttribute("maleDosePercent", df.format(malePercentage));
        model.addAttribute("femaleDosePercent", df.format(femalePercentage));
        //model.addAttribute("maleDosePercent", male * 100.0 / (double) total);
        //model.addAttribute("femaleDosePercent", female * 100.0 / (double) total);

        // Calculate age distribution
        List<String> ageRangeLabels = List.of("18-25", "26-35", "36-45", "46-55", "56-65", "65+");
        List<Double> ageRangeValues = ageRangeLabels.stream()
                .map(range -> calculateAgeRangePercentage(range, total, users))
                .collect(Collectors.toList());

        model.addAttribute("ageRangeLabels", ageRangeLabels);
        model.addAttribute("ageRangeValues", ageRangeValues);

        // Calculate nationality distribution
        List<String> nationalityLabels = users.stream()
                .map(User::getNationality)
                .distinct()
                .collect(Collectors.toList());
        List<Long> nationalityValues = nationalityLabels.stream()
                .map(nationality -> (long) users.stream().filter(user -> user.getNationality().equalsIgnoreCase(nationality)).count())
                .collect(Collectors.toList());

        model.addAttribute("nationalityLabels", nationalityLabels);
        model.addAttribute("nationalityValues", nationalityValues);
    }

    private double calculateAgeRangePercentage(String range, long total, List<User> users) {
        int minAge;
        int maxAge;
        if (range.contains("+")) {
            minAge = Integer.parseInt(range.split("\\+")[0]);
            maxAge = Integer.MAX_VALUE;
        } else {
            minAge = Integer.parseInt(range.split("-")[0]);
            maxAge = Integer.parseInt(range.split("-")[1]);
        }

        long count = users.stream()
                .filter(user -> {
                    int age = calculateAge(user.getDateOfBirth());
                    return age >= minAge && age <= maxAge;
                })
                .count();
        //return (double) count * 100 / total;   //Percentage
        return (int) count;
    }

    private int calculateAge(String dateOfBirth) {
        LocalDate dob = LocalDate.parse(dateOfBirth, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return Period.between(dob, LocalDate.now()).getYears();
    }

    @PostMapping("/stats")
    public String statistics(Model model, @RequestParam("nationality") String country) {
        // Populate the model with statistics for the selected country
        getStats(model, country);
        return "stats";
    }


    /*******User Area**********/
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("userSession", userSession);
        return "login";
    }


    @PostMapping("/login")
    public String login(@RequestParam("email") String email, @RequestParam("pps") String pps,
                        @RequestParam(value = "otp", required = false) String otp,
                        RedirectAttributes redirectAttributes, Model model) {
        // Ensure the user is found in the database by PPS and email
        User user = userRepository.findByEmail(email);
        if (user == null) {
            logger.error("Email is not registered.");
            redirectAttributes.addFlashAttribute("error", "Email is not registered.");
            return "redirect:/login";
        }

        // Check if the provided PPS matches the stored hashed PPS
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if (!passwordEncoder.matches(pps, user.getPPS())) {
            logger.error("Wrong PPS for {}", user);
            redirectAttributes.addFlashAttribute("error", "Wrong PPS.");
            return "redirect:/login";
        }


        // Handle MFA-enabled users
        if (user.isMfaEnabled()) {
            if (otp == null || otp.isEmpty()) {
                logger.error("OTP is required for users with MFA {}", user);
                redirectAttributes.addFlashAttribute("error", "OTP is required for users with MFA enabled.");
                return "redirect:/login";
            }
            if (!mfaService.validateOtp(user.getMfaSecret(), otp)) {
                logger.error("Invalid OTP. Please try again. {}", user);
                redirectAttributes.addFlashAttribute("error", "Invalid OTP. Please try again.");
                return "redirect:/login";
            }
        }

        userSession.setUserId(user.getId());
        redirectAttributes.addFlashAttribute("success", "Welcome, " + user.getFullName() + "!");
        return "redirect:/profile";
    }



    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("userSession", userSession);
        return "register";
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String register(@Valid @ModelAttribute("user") User user, BindingResult result, RedirectAttributes redirectAttributes, Model model) {
        // Validate user input
        if (user.getDateOfBirth().isEmpty() || user.getEmail().isEmpty() || user.getAddress().isEmpty()
                || user.getFullName().isEmpty() || user.getGender().isEmpty() || user.getNationality().isEmpty()
                || user.getPhoneNumber().isEmpty() || user.getPPS().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "All fields are required!");
            return "redirect:/register";
        }

        // Sanitize inputs before processing
        user.setEmail(validationService.sanitizeInput(user.getEmail()));
        user.setPPS(validationService.sanitizeInput(user.getPPS()));


        // Assign a default role if it's not set
        if (user.getRole() == null) {
            user.setRole("USER"); // Assign a default role, e.g., ROLE_USER
        }

        if (userRepository.findByPPS(user.getPPS()) != null) {
            redirectAttributes.addFlashAttribute("error", "User with this PPS number already exists.");
            return "redirect:/register";
        }
        if (userRepository.findByEmail(user.getEmail()) != null) {
            redirectAttributes.addFlashAttribute("error", "User with this email already exists.");
            return "redirect:/register";
        }
        // Ensure user is 18 or older
        if (isUserUnderage(user.getDateOfBirth())) {
            redirectAttributes.addFlashAttribute("error", "Users under 18 cannot create an account.");
            return "redirect:/register";
        }
        // Encrypt the password before saving
        user.setPPS(ppsEncoder.encode(user.getPPS()));
        user.setMfaEnabled(true);
        // Save the user in the database
        userRepository.save(user);

        // Set the userId in the session
        userSession.setUserId(user.getId());

        // Generate MFA secret if enabled
        if (user.isMfaEnabled()) {
            String secret = mfaService.generateSecret();
            user.setMfaSecret(secret);
            userRepository.save(user);  // Save the updated user with MFA secret

            // Generate QR code URL for MFA setup
            String qrUrl = mfaService.getQrCodeUrl(secret, user.getEmail());
            model.addAttribute("qrUrl", qrUrl);
            redirectAttributes.addFlashAttribute("success", "Account created! Please set up MFA.");
            return "mfasetup";
        }

        redirectAttributes.addFlashAttribute("success", "Account and MFA setup! Welcome, " + user.getFullName() + "!");
        return "redirect:/login";
    }

    @PostMapping("/verify-mfa")
    public String verifyMfa(@RequestParam("otp") String otp, RedirectAttributes redirectAttributes, Model model) {
        User user = userSession.getUser();
        MFAService mfaService = new MFAService();

        // Ensure that the OTP is numeric
        if (!otp.matches("\\d+")) {
            model.addAttribute("qrUrl", mfaService.getQrCodeUrl(user.getMfaSecret(), user.getEmail()));
            model.addAttribute("error", "Invalid OTP. Please enter a numeric OTP.");
            return "mfasetup";
        }

        if (mfaService.validateOtp(user.getMfaSecret(), otp)) {
            redirectAttributes.addFlashAttribute("success", "MFA setup complete! You are now logged in.");
            return "redirect:/profile";
        } else {
            model.addAttribute("qrUrl", mfaService.getQrCodeUrl(user.getMfaSecret(), user.getEmail()));
            redirectAttributes.addFlashAttribute("error", "Invalid OTP. Please try again.");
            return "mfasetup";
        }


    }

    @PostMapping("/enable-mfa")
    public String enableMfa(RedirectAttributes redirectAttributes, Model model) {
        User user = userSession.getUser();
        MFAService mfaService = new MFAService();

        String secret = mfaService.generateSecret();
        user.setMfaSecret(secret);
        user.setMfaEnabled(true);
        userRepository.save(user);

        String qrUrl = mfaService.getQrCodeUrl(secret, user.getEmail());
        model.addAttribute("qrUrl", qrUrl);
        System.out.println("QR Code URL: " + qrUrl);
        return "mfasetup";
    }



    @GetMapping("/logout")
    public String logout() {
        userSession.setUserId(null);
        return "redirect:/";
    }

    @GetMapping("/forum")
    public String forum(Model model) {
        // Retrieve all questions and answers from the database
        List<ForumQuestion> questions = forumQuestionRepository.findAll();
        model.addAttribute("questions", questions);
        model.addAttribute("userSession", userSession);
        return "forum";
    }

    @GetMapping("/ask-a-question")
    public String askAQuestion(Model model, RedirectAttributes redirectAttributes) {
        // If not logged in or admin, return to forum
        if (!userSession.isLoggedIn() || userSession.getUser().isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Users must be logged in to ask questions.");
            return "redirect:/forum";
        }
        // If user, return ask-a-question page
        model.addAttribute("userSession", userSession);
        return "ask-a-question";
    }

    @PostMapping("/ask-a-question")
    public String askAQuestion(@RequestParam String title, @RequestParam String details, Model model,
                               RedirectAttributes redirectAttributes) {
        // If user is not logged in or is admin
        if (!userSession.isLoggedIn() || userSession.getUser().isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Users must be logged in to ask questions.");
            return "redirect:/forum";
        }

        // Create new question entry in the database
        ForumQuestion newQuestion = new ForumQuestion(title, details, getDateSubmitted(), userSession.getUser());

        // Add question to the database
        forumQuestionRepository.save(newQuestion);

        redirectAttributes.addFlashAttribute("success", "The question was successfully submitted.");

        // Redirect to the new question page
        return "redirect:/question?id=" + newQuestion.getId();
    }

    @PostMapping("/question")
    public String answerQuestion(@RequestParam String body, @RequestParam String id, Model model,
                                 RedirectAttributes redirectAttributes) {
        // Retrieve the question
        try {
            Integer questionId = Integer.parseInt(id);
            Optional<ForumQuestion> question = forumQuestionRepository.findById(questionId);
            if (question.isPresent()) {
                // If user is admin
                if (userSession.isLoggedIn() && userSession.getUser() != null && userSession.getUser().isAdmin()) {
                    // Create a new answer entry in the database
                    ForumAnswer newAnswer = new ForumAnswer(body, getDateSubmitted());
                    // Save forum question and answer
                    newAnswer.setAdmin(userSession.getUser());
                    newAnswer.setQuestion(question.get());
                    forumAnswerRepository.save(newAnswer);
                    question.get().addAnswer(newAnswer);
                    forumQuestionRepository.save(question.get());

                    redirectAttributes.addFlashAttribute("success", "The answer was successfully submitted.");
                    // Redirect to the updated question page
                    return "redirect:/question?id=" + question.get().getId();
                } else {
                    redirectAttributes.addFlashAttribute("error",
                            "Only admins may answer questions. If you are an admin, please log in.");
                    // Redirect to the unchanged same question page
                    return "redirect:/question?id=" + question.get().getId();
                }
            }

        } catch (NumberFormatException e) {
            return "redirect:/forum";
        }
        return "redirect:/forum";
    }



    @PostMapping("/mark-answer")
    public String markAnswer(@RequestParam("answerId") Integer answerId, @RequestParam("isHelpful") Boolean isHelpful,
                             RedirectAttributes redirectAttributes) {
        if (!userSession.isLoggedIn()) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to give feedback.");
            return "redirect:/question?id=" + answerId;
        }

        Optional<ForumAnswer> optionalAnswer = forumAnswerRepository.findById(answerId);
        if (optionalAnswer.isPresent()) {
            ForumAnswer answer = optionalAnswer.get();
            User user = userSession.getUser();

            Optional<UserAnswerFeedback> existingFeedback = userAnswerFeedbackRepository.findByUserAndAnswer(user, answer);
            if (existingFeedback.isPresent()) {
                // User already gave feedback, remove it
                userAnswerFeedbackRepository.delete(existingFeedback.get());
                if (existingFeedback.get().getFeedbackType().equals("helpful")) {
                    answer.setHelpfulCount(answer.getHelpfulCount() - 1);
                } else {
                    answer.setNotHelpfulCount(answer.getNotHelpfulCount() - 1);
                }
            } else {
                // Add new feedback
                UserAnswerFeedback feedback = new UserAnswerFeedback();
                feedback.setUser(user);
                feedback.setAnswer(answer);
                feedback.setFeedbackType(isHelpful ? "helpful" : "not_helpful");

                userAnswerFeedbackRepository.save(feedback);
                if (isHelpful) {
                    answer.setHelpfulCount(answer.getHelpfulCount() + 1);
                } else {
                    answer.setNotHelpfulCount(answer.getNotHelpfulCount() + 1);
                }
            }

            forumAnswerRepository.save(answer);
            redirectAttributes.addFlashAttribute("success", "Your feedback has been recorded.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Answer not found.");
        }
        return "redirect:/question?id=" + answerId;
    }



    @GetMapping("/profile")
    public String profile(Model model, RedirectAttributes redirectAttributes) {
        if (!userSession.isLoggedIn()) {
            redirectAttributes.addFlashAttribute("error",
                    "You must be logged in to view your profile. If you do not already have an account, please register.");
            return "redirect:/login";
        }

        List<Appointment> apps = appointmentRepository.findByUser(userSession.getUserId());
        Collections.reverse(apps);

        List<Vaccine> vaxes = vaccineRepository.findByUser(userSession.getUserId());
        Collections.reverse(vaxes);

        model.addAttribute("vaccineCenters", vaccineCentreRepository.findAll());
        model.addAttribute("appointments", apps);
        model.addAttribute("vaccines", vaxes);
        model.addAttribute("userSession", userSession);
        model.addAttribute("userProfile", userSession.getUser());
        model.addAttribute("isSelf", true);
        model.addAttribute("userDoses", vaxes.size());
        model.addAttribute("userQuestions", forumQuestionRepository.findByUser(userSession.getUserId()).size());
        model.addAttribute("userAppts", appointmentRepository.findByUser(userSession.getUserId()).size());
        return "profile";
    }

    @GetMapping("/profile/{stringId}")
    public String profile(@PathVariable String stringId, Model model) {
        if (stringId == null)
            return "404";

        try {
            Integer id = Integer.valueOf(stringId);
            Optional<User> user = userRepository.findById(id);

            if (!user.isPresent()) {
                return "404";
            }
            User currentUser = userSession.getUser();
            if (currentUser == null) {
                return "redirect:/login"; // User must be logged in to view a profile
            }

            // Check if the current user is either viewing their own profile or is an admin
            if (!currentUser.isAdmin() && !currentUser.getId().equals(user.get().getId())) {
                // Unauthorized access attempt detected
                return "404"; // Redirect to an access denied page
            }


            List<Vaccine> vaxes = vaccineRepository.findByUser(user.get().getId());

            if (userSession.isLoggedIn() && userSession.getUser().isAdmin()) {
                // Admins can see everybody's appointments
                List<Appointment> apps = appointmentRepository.findByUser(user.get().getId());
                Collections.reverse(apps);
                Collections.reverse(vaxes);

                model.addAttribute("appointments", apps);
                model.addAttribute("vaccines", vaxes);
            }

            model.addAttribute("vaccineCenters", vaccineCentreRepository.findAll());
            model.addAttribute("userSession", userSession);
            model.addAttribute("userProfile", user.get());
            model.addAttribute("userQuestions", forumQuestionRepository.findByUser(user.get().getId()).size());
            model.addAttribute("userDoses", vaxes.size());
            model.addAttribute("userAppts", appointmentRepository.findByUser(user.get().getId()).size());
            return "profile";
        } catch (NumberFormatException ex) {
            return "404";
        }
    }



    @GetMapping("/cancel-appointment/{stringId}")
    public String cancelAppointment(@PathVariable String stringId, RedirectAttributes redirectAttributes) {
        if (!userSession.isLoggedIn())
            return "redirect:/login";

        Integer id = Integer.valueOf(stringId);
        Appointment app = appointmentRepository.findById(id).get();

        if (!userSession.getUser().isAdmin() && !userSession.getUser().getId().equals(app.getUser().getId())) {
            // Hacker detected! You can't cancel someone else's appointment!
            return "404";
        }

        app.setStatus("cancelled");
        appointmentRepository.save(app);

        AppointmentSlot appSlot = new AppointmentSlot(app.getVaccineCentre(), app.getDate(), app.getTime());
        appointmentSlotRepository.save(appSlot);

        redirectAttributes.addFlashAttribute("success", "The appointment was successfully cancelled.");

        if (!app.getUser().getId().equals(userSession.getUser().getId())) {
            return "redirect:/profile/" + app.getUser().getId();
        }

        return "redirect:/profile";
    }

    @GetMapping("/question")
    public String getQuestionById(@RequestParam(name = "id") Integer id, Model model,
                                  RedirectAttributes redirectAttributes) {
        // Retrieve question
        Optional<ForumQuestion> question = forumQuestionRepository.findById(id);
        if (question.isPresent()) {
            // Return question information
            model.addAttribute("question", question.get());
            model.addAttribute("userSession", userSession);
            return "question.html";
        } else {
            redirectAttributes.addFlashAttribute("error", "The question you requested could not be found.");
            // Redirect if question not found
            return "redirect:/forum";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        if (!userSession.isLoggedIn() || !userSession.getUser().isAdmin())
            return "redirect:/login";

        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("userSession", userSession);
        return "dashboard";
    }

    @PostMapping(value = "/find-user")
    public String findUser(@RequestParam Map<String, String> body, Model model) {
        String input = body.get("input");

        User user = userRepository.findByPPSorName(input);
        if (user == null) {
            return "redirect:/dashboard";
        }

        return "redirect:/profile/" + user.getId();
    }

    @PostMapping(value = "/assign-vaccine")
    public String assignVaccine(@RequestParam Map<String, String> body, Model model,
                                RedirectAttributes redirectAttributes) {
        if (!userSession.isLoggedIn() || !userSession.getUser().isAdmin()) {
            return "redirect:/login";
        }

        LocalDate vaxDate = LocalDate.parse(body.get("date"), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        Integer userId = Integer.valueOf(body.get("user_id"));
        Integer centreId = Integer.valueOf(body.get("center_id"));
        String vaxType = body.get("vaccine");

        User vaxUser = userRepository.findById(userId).get();
        VaccineCentre vaxCentre = vaccineCentreRepository.findById(centreId).get();
        redirectAttributes.addFlashAttribute("success", "The vaccine was recorded.");

        // See how many other doses there are per user
        List<Vaccine> vaccines = vaccineRepository.findByUser(userId);
        if (vaccines == null || vaccines.size() == 0) {
            // Getting date in 3 weeks for second vaccination between 9 and 17
            LocalDate date = vaxDate.plusDays(21);
            LocalTime time = LocalTime.of(9, 00, 00);
            Appointment appointment = appointmentRepository.findByDetails(centreId, date, time);
            while (appointment != null) {
                time = time.plusMinutes(15);
                if (time.getHour() > 17) {
                    if (date.getDayOfWeek() == DayOfWeek.FRIDAY) {
                        date = date.plusDays(3);
                    } else {
                        date = date.plusDays(1);
                    }
                    time = LocalTime.of(9, 00, 00);
                }
                appointment = appointmentRepository.findByDetails(centreId, date, time);
            }
            User user = userRepository.findById(userId).get();
            // Creating new appointment for the user
            appointment = new Appointment(vaxCentre, date, time, user, "pending");
            appointmentRepository.save(appointment);
            redirectAttributes.addFlashAttribute("success",
                    "The vaccine was recorded and a new appointment at least 3 weeks from now has been made for the user.");
        }
        // Save new vaccine
        Vaccine vax = new Vaccine(userSession.getUser(), vaxDate, vaxCentre, vaxUser, vaxType);
        vaccineRepository.save(vax);

        return "redirect:/profile/" + userId;
    }

    @GetMapping("/complete-appointment/{stringId}")
    public String completeAppointment(@PathVariable String stringId, RedirectAttributes redirectAttributes) {
        if (!userSession.isLoggedIn())
            return "redirect:/login";

        if (!userSession.getUser().isAdmin()) {
            // Hacker detected! You can't modify if you're not an admin!
            return "404";
        }

        Integer id = Integer.valueOf(stringId);
        Appointment app = appointmentRepository.findById(id).get();

        app.setStatus("done");
        appointmentRepository.save(app);

        redirectAttributes.addFlashAttribute("success", "The appointment was marked as complete.");

        if (!app.getUser().getId().equals(userSession.getUser().getId())) {
            return "redirect:/profile/" + app.getUser().getId();
        }

        return "redirect:/profile";
    }


    @PostMapping("/upload-yaml")
    public String uploadYaml(@RequestParam("yamlContent") String yamlContent) {
        // Validate and parse the YAML content
        if (!yamlHandler.isValidYaml(yamlContent)) {
            return "404"; // Show an error page
        }

        Object parsedData = yamlHandler.parseYaml(yamlContent);
        if (parsedData == null) {
            return "404"; // Show an error page
        }

        // Further processing of the parsed YAML data
        return "/dashboard"; // Show success page
    }


    /**
     * /########################
     * <p>
     * Helpers
     * </p>
     * /#######################
     */

    private String getDateSubmitted() {
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return currentDate.format(formatter);
    }

    private boolean isUserUnderage(String dateOfBirth) {
        LocalDate dob = LocalDate.parse(dateOfBirth, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return Period.between(dob, LocalDate.now()).getYears() < 18;
    }
}