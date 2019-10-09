package ai.tomorrow.mywallet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val SAMPLE_MNEMONIC1 = "world tired copper write maid monkey risk today husband hope grid inflict"
    private val SAMPLE_MNEMONIC2 = "mouse inject office junior repeat one tip actor drift love auto chase"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        sampleMnemonicBtn1.setOnClickListener { mnemonicEditText1.setText(SAMPLE_MNEMONIC1) }
        sampleMnemonicBtn2.setOnClickListener { mnemonicEditText2.setText(SAMPLE_MNEMONIC2) }



    }
}
