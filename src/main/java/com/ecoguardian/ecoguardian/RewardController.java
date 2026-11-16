package com.ecoguardian.ecoguardian;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rewards")
@CrossOrigin
public class RewardController {

    private final RewardRepository rewardRepository;

    public RewardController(RewardRepository rewardRepository) {
        this.rewardRepository = rewardRepository;
    }


    // ==========================================
    // GET ALL REWARDS
    // ==========================================

    @GetMapping
    public List<Reward> getRewards() {

        return rewardRepository.findAll();
    }


    // ==========================================
    // GET USER REWARDS
    // ==========================================

    @GetMapping("/users")
    public List<Reward> getUserRewards() {

        return rewardRepository.findAll()
                .stream()
                .filter(reward ->
                        "USER".equalsIgnoreCase(reward.getRole()))
                .toList();
    }


    // ==========================================
    // GET WORKER REWARDS
    // ==========================================

    @GetMapping("/workers")
    public List<Reward> getWorkerRewards() {

        return rewardRepository.findAll()
                .stream()
                .filter(reward ->
                        "WORKER".equalsIgnoreCase(reward.getRole()))
                .toList();
    }


    // ==========================================
    // ADD USER / WORKER
    // ==========================================

    @PostMapping
    public Reward addReward(@RequestBody Reward reward) {

        return rewardRepository.save(reward);
    }


    // ==========================================
    // UPDATE NAME AND POINTS
    // ADMIN
    // ==========================================

    @PutMapping("/{id}")
    public Reward updateReward(
            @PathVariable Long id,
            @RequestBody Reward updatedReward) {

        Reward reward =
                rewardRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Reward record not found"));

        reward.setName(updatedReward.getName());
        reward.setRole(updatedReward.getRole());
        reward.setPoints(updatedReward.getPoints());

        return rewardRepository.save(reward);
    }


    // ==========================================
    // ADD POINTS
    // ==========================================

    @PutMapping("/{id}/points")
    public Reward addPoints(
            @PathVariable Long id,
            @RequestParam int points) {

        Reward reward =
                rewardRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Reward record not found"));

        reward.setPoints(
                reward.getPoints() + points
        );

        return rewardRepository.save(reward);
    }


    // ==========================================
    // DELETE REWARD RECORD
    // ADMIN
    // ==========================================

    @DeleteMapping("/{id}")
    public void deleteReward(
            @PathVariable Long id) {

        rewardRepository.deleteById(id);
    }
}