/*
	* Copyright (c) 2013, ABZ Reporting GmbH (www.abz-reporting.com) 
	* All rights reserved.
	*
	* This software is the confidential and proprietary information
	* of ABZ Reporting GmbH ("Confidential Information").  You shall not
	* disclose such Confidential Information and shall use it only
	* in accordance with the terms of the license agreement you
	* entered into with ABZ Reporting GmbH.
	*
	* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
	* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
	* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
	* DISCLAIMED.  IN NO EVENT SHALL ABZ REPORTING GMBH OR
	* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
	* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
	* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
	* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
	* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
	* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
	* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
	* SUCH DAMAGE.
	* 
	* The original code is ABRA, an XBRL processor.
	* The Initial Developer of the Original Code is Thomas Klement
	* <thomas.klement@abz-reporting.com>.
	* Contributor(s): Harald Schmitt, Predrag Knezevic
	*
 */

package org.grails.plugins.marshallers.config

/**
 * @author Predrag Knezevic
 */
class MarshallingConfigPool {

    def configs = [:]
    MarshallingConfigPool parentPool
    
    def MarshallingConfigPool(configs) {
        for (c in configs) {
            this.configs.put(c.clazz, c)
        }
    }
    
    def get(Class clazz, boolean useParent = false) {
        def c = configs.get(clazz)
        if (c == null && parentPool != null && useParent) {
            c = parentPool.get(clazz, true)
        }    
        c
    }
    
}
