package tech.relaycorp.ping.ui.ping

import android.content.ClipboardManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.schibsted.spain.barista.assertion.BaristaContentDescriptionAssertions.assertContentDescription
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.relaycorp.ping.R
import tech.relaycorp.ping.data.database.dao.PingDao
import tech.relaycorp.ping.data.database.dao.PublicPeerDao
import tech.relaycorp.ping.test.AppTestProvider.component
import tech.relaycorp.ping.test.AppTestProvider.context
import tech.relaycorp.ping.test.BaseActivityTestRule
import tech.relaycorp.ping.test.PingEntityFactory
import tech.relaycorp.ping.test.PublicPeerEntityFactory
import tech.relaycorp.ping.ui.common.DateTimeFormat
import java.time.ZonedDateTime
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class PingActivityTest {

    @Rule
    @JvmField
    val testRule = BaseActivityTestRule(PingActivity::class, false)

    @Inject
    lateinit var publicPeerDao: PublicPeerDao

    @Inject
    lateinit var pingDao: PingDao

    @Before
    fun setUp() {
        component.inject(this)
    }

    @Test
    fun displaysPing() {
        val peer = PublicPeerEntityFactory.build()
        val ping = PingEntityFactory.build(peer)
        runBlocking {
            publicPeerDao.save(peer)
            pingDao.save(ping)
        }

        testRule.start(PingActivity.getIntent(context, ping.pingId))

        assertDisplayed(peer.publicAddress)
        assertDisplayed(ping.pingId)
        assertContentDescription(R.id.state, R.string.ping_state_sent)
        assertDisplayed(DateTimeFormat.format(ping.sentAt))
        assertDisplayed(DateTimeFormat.format(ping.expiresAt))
        assertNotDisplayed(R.string.ping_pong_received_at)
    }

    @Test
    fun displaysPingWithPong() {
        val peer = PublicPeerEntityFactory.build()
        val ping = PingEntityFactory.build(peer).copy(
            pongReceivedAt = ZonedDateTime.now()
        )
        runBlocking {
            publicPeerDao.save(peer)
            pingDao.save(ping)
        }

        testRule.start(PingActivity.getIntent(context, ping.pingId))

        assertDisplayed(peer.publicAddress)
        assertDisplayed(ping.pingId)
        assertContentDescription(R.id.state, R.string.ping_state_replied)
        assertDisplayed(DateTimeFormat.format(ping.sentAt))
        assertDisplayed(DateTimeFormat.format(ping.expiresAt))
        assertDisplayed(DateTimeFormat.format(ping.pongReceivedAt))
    }

    @Test
    fun copyPingId() {
        val peer = PublicPeerEntityFactory.build()
        val ping = PingEntityFactory.build(peer)
        runBlocking {
            publicPeerDao.save(peer)
            pingDao.save(ping)
        }

        testRule.start(PingActivity.getIntent(context, ping.pingId))

        clickOn(R.id.copy)
        assertDisplayed(R.string.copy_confirm)

        runBlocking(Dispatchers.Main) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals(
                ping.pingId,
                clipboard.primaryClip?.getItemAt(0)?.text.toString()
            )
        }
    }
}
