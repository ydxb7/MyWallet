package ai.tomorrow.mywallet

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.consenlabs.tokencore.wallet.Identity
import org.consenlabs.tokencore.wallet.KeystoreStorage
import org.consenlabs.tokencore.wallet.Wallet
import org.consenlabs.tokencore.wallet.WalletManager
import org.consenlabs.tokencore.wallet.model.ChainType
import org.consenlabs.tokencore.wallet.model.Metadata
import org.consenlabs.tokencore.wallet.model.Network
import org.consenlabs.tokencore.wallet.transaction.EthereumTransaction
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.utils.Numeric
import java.nio.file.Path
import java.nio.file.Paths


class MainActivity : AppCompatActivity(), KeystoreStorage {


    private val TAG = "MainActivity"
    private val SAMPLE_MNEMONIC1 = "world tired copper write maid monkey risk today husband hope grid inflict"
    private val SAMPLE_MNEMONIC2 = "mouse inject office junior repeat one tip actor drift love auto chase"

    private val web3j = Web3j.build(HttpService("https://ropsten.infura.io/llyrtzQ3YhkdESt2Fzrk"))

    private var uiHandler = Handler()
    private lateinit var backgroundHandler: Handler
    private lateinit var backgroundThread: HandlerThread

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WalletManager.storage = this
        WalletManager.scanWallets()

//        web3j = Web3j.build(HttpService("https://ropsten.infura.io/llyrtzQ3YhkdESt2Fzrk", httpClient, false))

        sampleMnemonicBtn1.setOnClickListener { mnemonicEditText1.setText(SAMPLE_MNEMONIC1) }
        sampleMnemonicBtn2.setOnClickListener { mnemonicEditText2.setText(SAMPLE_MNEMONIC2) }




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

        // get balance
        Log.d(TAG, "balance: ${getBalance(ethereumWallet1.address)}")
        Log.d(TAG, "balance: ${getBalance(ethereumWallet2.address)}")

        // send
        // keystore 文件路径
        val directory = File(filesDir, "wallets")
        val f = File(directory, "${ethereumWallet1.id}.json")
        Log.d(TAG, "is file exist: ${f.exists()}")

        send(ethereumWallet1, ethereumWallet2.address)


    }

    private fun send(fromWallet: Wallet, toAddress: String){
        uiScope.launch {

            withContext(Dispatchers.IO){
                val ethGetTransactionCount = web3j.ethGetTransactionCount(
                    "0x${fromWallet.address}", DefaultBlockParameterName.LATEST
                ).send()
                val nonce = ethGetTransactionCount.transactionCount
                val gasLimit = BigInteger.valueOf(21000L)
//                val amount = BigInteger.valueOf(1)
                val amount = Convert.toWei(BigDecimal(1L), Convert.Unit.ETHER).toBigInteger()
                val gasPrice = web3j.ethGasPrice().send().gasPrice
                Log.d(TAG, "gasPrice: $gasPrice")
                Log.d(TAG, "nonce: $nonce")

                val keystorePath = filesDir.absolutePath + "/wallets" + "/${fromWallet.id}.json"
                val credentials = WalletUtils.loadCredentials("", keystorePath)

                val rawTransaction = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toAddress, amount)
                val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)

                val hexValue = Numeric.toHexString(signedMessage)
                val ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send()

                Log.d(TAG, ethSendTransaction.toString())
                Log.d(TAG, "transactionHash: ${ethSendTransaction.transactionHash}")

            }
        }
    }


    private fun getBalance(address: String): BigDecimal{
        // connect to node
//        val web3 = Web3j.build(HttpService("https://ropsten.infura.io/llyrtzQ3YhkdESt2Fzrk"))  // defaults to http://localhost:8545/

        // send asynchronous requests to get balance
        val ethGetBalance = web3j
            .ethGetBalance("0x$address", DefaultBlockParameterName.LATEST)
            .sendAsync()
            .get()

        val wei = ethGetBalance.balance
        return Convert.fromWei(BigDecimal(wei), Convert.Unit.ETHER)
    }


    override fun getKeystoreDir(): File {
//        Log.d(TAG, "getKeystoreDir = $filesDir")
        return this.filesDir
    }
}
