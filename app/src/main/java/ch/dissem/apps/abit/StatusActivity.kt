/*
 * Copyright 2016 Christian Basler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.dissem.apps.abit

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import ch.dissem.apps.abit.service.Singleton
import com.mikepenz.materialize.MaterializeBuilder
import kotlinx.android.synthetic.main.activity_status.*

class StatusActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(false)

        MaterializeBuilder()
                .withActivity(this)
                .withStatusBarColorRes(R.color.colorPrimaryDark)
                .withTranslucentStatusBarProgrammatically(true)
                .withStatusBarPadding(true)
                .build()

        val bmc = Singleton.getBitmessageContext(this)
        val status = StringBuilder()
        for (address in bmc.addresses.getIdentities()) {
            status.append(address.address).append('\n')
        }
        status.append('\n')
        status.append(bmc.status())
        content.text = status
    }

}
