package info.nightscout.androidaps.plugins.pump.omnipod.dash.driver.pod.state

import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.plugins.pump.omnipod.dash.driver.comm.Id
import info.nightscout.androidaps.plugins.pump.omnipod.dash.driver.comm.pair.PairResult
import info.nightscout.androidaps.plugins.pump.omnipod.dash.driver.event.PodEvent
import info.nightscout.androidaps.plugins.pump.omnipod.dash.driver.pod.definition.*
import info.nightscout.androidaps.plugins.pump.omnipod.dash.driver.pod.response.AlarmStatusResponse
import info.nightscout.androidaps.plugins.pump.omnipod.dash.driver.pod.response.DefaultStatusResponse
import info.nightscout.androidaps.plugins.pump.omnipod.dash.driver.pod.response.SetUniqueIdResponse
import info.nightscout.androidaps.plugins.pump.omnipod.dash.driver.pod.response.VersionResponse
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Duration
import java.io.Serializable
import java.util.*

sealed class CommandConfirmationFromState
object CommandSendingFailure : CommandConfirmationFromState()
object CommandSendingNotConfirmed : CommandConfirmationFromState()
object CommandConfirmationDenied : CommandConfirmationFromState()
object CommandConfirmationSuccess : CommandConfirmationFromState()
object NoActiveCommand : CommandConfirmationFromState()

interface OmnipodDashPodStateManager {

    var activationProgress: ActivationProgress
    val isUniqueIdSet: Boolean
    val isActivationCompleted: Boolean
    val isSuspended: Boolean
    val isPodRunning: Boolean
    val isPodKaput: Boolean
    var bluetoothConnectionState: BluetoothConnectionState

    var timeZone: DateTimeZone
    val lastUpdatedSystem: Long // System.currentTimeMillis()
    val lastStatusResponseReceived: Long
    val time: DateTime?
    val timeBehind: Duration?

    val messageSequenceNumber: Short
    val sequenceNumberOfLastProgrammingCommand: Short?
    val activationTime: Long?
    var uniqueId: Long? // TODO make Int
    var bluetoothAddress: String?
    var ltk: ByteArray?
    var eapAkaSequenceNumber: Long

    val bluetoothVersion: SoftwareVersion?
    val firmwareVersion: SoftwareVersion?
    val lotNumber: Long?
    val podSequenceNumber: Long?
    val pulseRate: Short?
    val primePulseRate: Short?
    val podLifeInHours: Short?
    val firstPrimeBolusVolume: Short?
    val secondPrimeBolusVolume: Short?

    val pulsesDelivered: Short?
    val pulsesRemaining: Short?
    val podStatus: PodStatus?
    val deliveryStatus: DeliveryStatus?
    val minutesSinceActivation: Short?
    val activeAlerts: EnumSet<AlertType>?
    val alarmType: AlarmType?

    var tempBasal: TempBasal?
    val tempBasalActive: Boolean
    var basalProgram: BasalProgram?
    val activeCommand: ActiveCommand?
    val lastBolus: LastBolus?

    fun increaseMessageSequenceNumber()
    fun increaseEapAkaSequenceNumber(): ByteArray
    fun commitEapAkaSequenceNumber()
    fun updateFromDefaultStatusResponse(response: DefaultStatusResponse)
    fun updateFromVersionResponse(response: VersionResponse)
    fun updateFromSetUniqueIdResponse(response: SetUniqueIdResponse)
    fun updateFromAlarmStatusResponse(response: AlarmStatusResponse)
    fun updateFromPairing(uniqueId: Id, pairResult: PairResult)
    fun reset()

    fun createActiveCommand(
        historyId: String,
        basalProgram: BasalProgram? = null,
        tempBasal: TempBasal? = null,
        requestedBolus: Double? = null
    ): Single<ActiveCommand>
    fun updateActiveCommand(): Maybe<CommandConfirmed>
    fun observeNoActiveCommand(b: Boolean): Observable<PodEvent>
    fun getCommandConfirmationFromState(): CommandConfirmationFromState

    fun createLastBolus(requestedUnits: Double, historyId: String, bolusType: DetailedBolusInfo.BolusType)
    fun markLastBolusComplete(): LastBolus?
    fun onStart()

    data class ActiveCommand(
        val sequence: Short,
        val createdRealtime: Long,
        var sentRealtime: Long = 0,
        val historyId: String,
        var sendError: Throwable?,
        var basalProgram: BasalProgram?,
        val tempBasal: TempBasal?,
        val requestedBolus: Double?
    )

    // TODO: set created to "now" on boot
    data class TempBasal(val startTime: Long, val rate: Double, val durationInMinutes: Short) : Serializable

    data class LastBolus(
        val startTime: Long,
        val requestedUnits: Double,
        var bolusUnitsRemaining: Double,
        var deliveryComplete: Boolean,
        val historyId: String,
        val bolusType: DetailedBolusInfo.BolusType
    ) {

        fun deliveredUnits(): Double? {
            return if (deliveryComplete) {
                requestedUnits - bolusUnitsRemaining
            } else {
                null
            }
        }

        fun markComplete(): Double {
            this.deliveryComplete = true
            return requestedUnits - bolusUnitsRemaining
        }
    }

    enum class BluetoothConnectionState {
        CONNECTING, CONNECTED, DISCONNECTED
    }
}
