package lab.ujumeonji.moco.service.challenge

import lab.ujumeonji.moco.adapter.DailyChallengeRepositoryAdapter
import lab.ujumeonji.moco.model.Challenge
import lab.ujumeonji.moco.model.DailyChallenge
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.random.Random

@Service
@Transactional
class DailyChallengeService(
    private val dailyChallengeRepositoryAdapter: DailyChallengeRepositoryAdapter,
    private val challengeService: ChallengeService,
) {
    private val logger = LoggerFactory.getLogger(DailyChallengeService::class.java)

    fun findAll(): List<DailyChallenge> = dailyChallengeRepositoryAdapter.findAll()

    fun findById(id: String): DailyChallenge? = dailyChallengeRepositoryAdapter.findById(id).orElse(null)

    fun findByDate(date: LocalDate): DailyChallenge? = dailyChallengeRepositoryAdapter.findByDate(date)

    fun findTodaysChallenge(): DailyChallenge? = dailyChallengeRepositoryAdapter.findByDate(LocalDate.now())

    fun findActiveChallenges(): List<DailyChallenge> = dailyChallengeRepositoryAdapter.findByIsActiveTrue()

    fun findByChallenge(challengeId: String): List<DailyChallenge> {
        val challenge = challengeService.findById(challengeId)
        if (challenge == null) {
            logger.warn("Challenge not found with ID: $challengeId")
            return emptyList()
        }

        return dailyChallengeRepositoryAdapter.findByChallengeId(challengeId)
    }

    fun findBetweenDates(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<DailyChallenge> = dailyChallengeRepositoryAdapter.findByDateBetween(startDate, endDate)

    fun createDailyChallenge(
        challengeId: String,
        date: LocalDate = LocalDate.now(),
        isActive: Boolean = true,
    ): DailyChallenge {
        val challenge =
            challengeService.findById(challengeId)
                ?: throw IllegalArgumentException("Challenge not found with ID: $challengeId")

        val existingDailyChallenge = dailyChallengeRepositoryAdapter.findByDate(date)
        if (existingDailyChallenge != null) {
            logger.warn("Daily challenge already exists for date: $date")
            return existingDailyChallenge
        }

        val dailyChallenge =
            DailyChallenge.create(
                challengeId = challengeId,
                date = date,
                isActive = isActive,
            )

        logger.info("Creating daily challenge for date: $date, challenge: ${challenge.title}")
        return dailyChallengeRepositoryAdapter.save(dailyChallenge)
    }

    fun updateDailyChallenge(
        id: String,
        updatedDailyChallenge: DailyChallenge,
    ): DailyChallenge? {
        val existingDailyChallenge = dailyChallengeRepositoryAdapter.findById(id).orElse(null)

        return if (existingDailyChallenge != null) {
            val dailyChallenge =
                DailyChallenge.create(
                    id = existingDailyChallenge.id,
                    challengeId = updatedDailyChallenge.challengeId,
                    date = updatedDailyChallenge.date,
                    isActive = updatedDailyChallenge.isActive,
                )
            dailyChallengeRepositoryAdapter.save(dailyChallenge)
        } else {
            null
        }
    }

    fun activateChallenge(
        id: String,
        isActive: Boolean = true,
    ): DailyChallenge? {
        val existingDailyChallenge = dailyChallengeRepositoryAdapter.findById(id).orElse(null)

        return if (existingDailyChallenge != null) {
            val dailyChallenge =
                DailyChallenge.create(
                    id = existingDailyChallenge.id,
                    challengeId = existingDailyChallenge.challengeId,
                    date = existingDailyChallenge.date,
                    isActive = isActive,
                )
            logger.info("Updating daily challenge active status: $id -> $isActive")
            dailyChallengeRepositoryAdapter.save(dailyChallenge)
        } else {
            null
        }
    }

    fun deleteById(id: String) {
        dailyChallengeRepositoryAdapter.deleteById(id)
    }

    fun deleteByChallengeId(challengeId: String) {
        val dailyChallenges = dailyChallengeRepositoryAdapter.findByChallengeId(challengeId)
        dailyChallengeRepositoryAdapter.deleteAll(dailyChallenges)
    }

    fun getOrCreateTodaysChallenge(challengeId: String): DailyChallenge {
        val today = LocalDate.now()
        val existingChallenge = dailyChallengeRepositoryAdapter.findByDate(today)

        return if (existingChallenge != null) {
            logger.info("Today's challenge already exists: ${existingChallenge.id}")
            existingChallenge
        } else {
            val challenge = challengeService.findById(challengeId)
            if (challenge != null) {
                logger.info("Creating today's challenge with challenge ID: $challengeId")
                createDailyChallenge(challengeId, today, true)
            } else {
                throw IllegalArgumentException("Challenge not found with ID: $challengeId")
            }
        }
    }

    fun findToday(): DailyChallenge? = dailyChallengeRepositoryAdapter.findByDate(LocalDate.now())

    fun findByDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<DailyChallenge> = dailyChallengeRepositoryAdapter.findByDateBetween(startDate, endDate)

    fun findChallengeForDate(date: LocalDate): Challenge? {
        val dailyChallenge = dailyChallengeRepositoryAdapter.findByDate(date)
        return if (dailyChallenge != null) {
            challengeService.findById(dailyChallenge.challengeId)
        } else {
            null
        }
    }

    fun findTodayChallenge(): Challenge? {
        val today = LocalDate.now()
        return findChallengeForDate(today)
    }

    fun save(dailyChallenge: DailyChallenge): DailyChallenge {
        val challenge = challengeService.findById(dailyChallenge.challengeId)
        if (challenge == null) {
            throw IllegalArgumentException("Challenge with ID ${dailyChallenge.challengeId} does not exist")
        }

        val existingDailyChallenge = dailyChallengeRepositoryAdapter.findByDate(dailyChallenge.date)
        if (existingDailyChallenge != null && existingDailyChallenge.id != dailyChallenge.id) {
            val updatedExisting =
                DailyChallenge.create(
                    id = existingDailyChallenge.id,
                    challengeId = existingDailyChallenge.challengeId,
                    date = existingDailyChallenge.date,
                    isActive = false,
                )
            dailyChallengeRepositoryAdapter.save(updatedExisting)
            logger.info("기존 일일 챌린지 비활성화: id = {}, date = {}", existingDailyChallenge.id, existingDailyChallenge.date)
        }

        logger.info("일일 챌린지 저장 중: 챌린지 ID = {}, 날짜 = {}", dailyChallenge.challengeId, dailyChallenge.date)
        return dailyChallengeRepositoryAdapter.save(dailyChallenge)
    }

    fun update(
        id: String,
        updatedDailyChallenge: DailyChallenge,
    ): DailyChallenge? {
        val existingDailyChallenge = dailyChallengeRepositoryAdapter.findById(id).orElse(null)

        return if (existingDailyChallenge != null) {
            val challenge = challengeService.findById(updatedDailyChallenge.challengeId)
            if (challenge == null) {
                throw IllegalArgumentException("Challenge with ID ${updatedDailyChallenge.challengeId} does not exist")
            }

            val dailyChallenge =
                DailyChallenge.create(
                    id = existingDailyChallenge.id,
                    challengeId = updatedDailyChallenge.challengeId,
                    date = updatedDailyChallenge.date,
                    isActive = updatedDailyChallenge.isActive,
                )
            dailyChallengeRepositoryAdapter.save(dailyChallenge)
        } else {
            null
        }
    }

    fun setDailyChallenge(
        date: LocalDate,
        challengeId: String,
    ): DailyChallenge {
        val challenge = challengeService.findById(challengeId)
        if (challenge == null) {
            throw IllegalArgumentException("Challenge with ID $challengeId does not exist")
        }

        val existingDailyChallenge = dailyChallengeRepositoryAdapter.findByDate(date)

        return if (existingDailyChallenge != null) {
            val updatedDailyChallenge =
                DailyChallenge.create(
                    id = existingDailyChallenge.id,
                    challengeId = challengeId,
                    date = existingDailyChallenge.date,
                    isActive = true,
                )
            logger.info("기존 일일 챌린지 업데이트: id = {}, date = {}", existingDailyChallenge.id, date)
            dailyChallengeRepositoryAdapter.save(updatedDailyChallenge)
        } else {
            val newDailyChallenge =
                DailyChallenge.create(
                    challengeId = challengeId,
                    date = date,
                    isActive = true,
                )
            logger.info("새 일일 챌린지 생성: 날짜 = {}", date)
            dailyChallengeRepositoryAdapter.save(newDailyChallenge)
        }
    }

    fun selectRandomChallenge(excludeRecentDays: Int = 7): Challenge? {
        val allChallenges = challengeService.findAll()
        if (allChallenges.isEmpty()) {
            logger.warn("사용 가능한 챌린지가 없습니다.")
            return null
        }

        val today = LocalDate.now()
        val startDate = today.minusDays(excludeRecentDays.toLong())
        val recentDailyChallenges = findBetweenDates(startDate, today)
        val recentChallengeIds = recentDailyChallenges.map { it.challengeId }.toSet()

        val availableChallenges =
            allChallenges.filter { challenge ->
                challenge.id != null && challenge.id !in recentChallengeIds
            }

        if (availableChallenges.isEmpty()) {
            logger.warn(
                "최근 {}일 동안 사용되지 않은 챌린지가 없습니다. 모든 챌린지에서 랜덤 선택합니다.",
                excludeRecentDays,
            )
            return allChallenges[Random.nextInt(allChallenges.size)]
        }

        val randomIndex = Random.nextInt(availableChallenges.size)
        return availableChallenges[randomIndex]
    }

    fun setRandomDailyChallenge(excludeRecentDays: Int = 7): DailyChallenge? {
        val randomChallenge = selectRandomChallenge(excludeRecentDays)

        return if (randomChallenge?.id != null) {
            val today = LocalDate.now()
            setDailyChallenge(today, randomChallenge.id as String)
        } else {
            logger.error("랜덤 챌린지를 선택할 수 없습니다.")
            null
        }
    }
}
