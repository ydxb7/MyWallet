package ai.tomorrow.mywallet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import org.consenlabs.tokencore.wallet.KeystoreStorage
import java.io.File
import org.consenlabs.tokencore.wallet.model.Metadata.P2WPKH
import android.provider.Telephony.Carriers.PASSWORD
import org.consenlabs.tokencore.wallet.Identity
import org.consenlabs.tokencore.wallet.Identity.createIdentity
import org.consenlabs.tokencore.wallet.model.Network
import org.consenlabs.tokencore.wallet.model.Metadata
import android.provider.Telephony.Carriers.PASSWORD
import android.util.Log
import okhttp3.OkHttpClient
import org.consenlabs.tokencore.wallet.Identity.recoverIdentity
import org.consenlabs.tokencore.wallet.WalletManager
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthGetBalance
import org.web3j.utils.Convert
import java.math.BigDecimal


class MainActivity : AppCompatActivity(), KeystoreStorage {


    private val TAG = "MainActivity"
    private val SAMPLE_MNEMONIC1 = "world tired copper write maid monkey risk today husband hope grid inflict"
    private val SAMPLE_MNEMONIC2 = "mouse inject office junior repeat one tip actor drift love auto chase"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        web3j = Web3j.build(HttpService("https://ropsten.infura.io/llyrtzQ3YhkdESt2Fzrk", httpClient, false))

        sampleMnemonicBtn1.setOnClickListener { mnemonicEditText1.setText(SAMPLE_MNEMONIC1) }
        sampleMnemonicBtn2.setOnClickListener { mnemonicEditText2.setText(SAMPLE_MNEMONIC2) }


        WalletManager.storage = this
        WalletManager.scanWallets()

        val identity1 = Identity.recoverIdentity(
            SAMPLE_MNEMONIC1,
            "identity1",
            "",
            "",
            Network.ROPSTEN,
            Metadata.NONE
        )

        val identity2 = Identity.recoverIdentity(
            SAMPLE_MNEMONIC2,
            "identity2",
            "",
            "",
            Network.ROPSTEN,
            Metadata.NONE
        )

        val ethereumWallet1 = identity1.wallets[0]
        val ethereumWallet2 = identity2.wallets[0]

        val prvKey1 = WalletManager.exportPrivateKey(ethereumWallet1.id, "")
        val prvKey2 = WalletManager.exportPrivateKey(ethereumWallet2.id, "")
        Log.d(TAG, "PrivateKey1: $prvKey1")
        Log.d(TAG, "PrivateKey2: $prvKey2")
        val mnemonic1 = WalletManager.exportMnemonic(ethereumWallet1.id, "").mnemonic
        val mnemonic2 = WalletManager.exportMnemonic(ethereumWallet2.id, "").mnemonic
        Log.d(TAG, "Mnemonic1: $mnemonic1")
        Log.d(TAG, "Mnemonic2: $mnemonic2")
        val json1 = WalletManager.exportKeystore(ethereumWallet1.id, "")
        val json2 = WalletManager.exportKeystore(ethereumWallet2.id, "")
        Log.d(TAG, "Keystore1: $json1")
        Log.d(TAG, "Keystore2: $json2")

        Log.d(TAG, "Adress1: ${ethereumWallet1.address}")
        Log.d(TAG, "Adress2: ${ethereumWallet2.address}")


        Log.d(TAG, "balance: ${getBalance(ethereumWallet1.address)}")
        Log.d(TAG, "balance: ${getBalance(ethereumWallet2.address)}")

    }

    private fun getBalance(address: String): BigDecimal{
        // connect to node
        val web3 = Web3j.build(HttpService("https://ropsten.infura.io/llyrtzQ3YhkdESt2Fzrk"))  // defaults to http://localhost:8545/

        // send asynchronous requests to get balance
        val ethGetBalance = web3
            .ethGetBalance("0x$address", DefaultBlockParameterName.LATEST)
            .sendAsync()
            .get()

        val wei = ethGetBalance.balance
        return Convert.fromWei(BigDecimal(wei), Convert.Unit.ETHER)
    }


    override fun getKeystoreDir(): File {
        return this.filesDir
    }
}
