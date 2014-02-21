/*
 * PointToPointRequest.java
 *
 * Created on 4 juillet 2008, 17:13
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.coronis.modules.requests;

import com.coronis.frames.*;
import com.coronis.modules.*;
import com.coronis.exception.*;

import java.io.IOException;

/**
 *
 * @author dpinte
 */
public class PointToPointServiceRequest extends Request {
    
    private boolean _subscribed = false;
    
    /** Creates a new instance of PointToPointRequest */
    public PointToPointServiceRequest(WavePort wpt, int[] msg, int allowedTime, String moduleId) {
        super(wpt, msg, allowedTime, moduleId);
    }
    
    public void subscribe() {
        // subscribe for ACK, RES_SEND_FRAME and RECEIVED_FRAME, CMD_ERROR and TR_ERROR_FRAME
        _wpt.subscribe(CoronisFrame.CMD_ACK, this);
        _wpt.subscribe(CoronisFrame.RES_SEND_SERVICE, this);
        _wpt.subscribe(CoronisFrame.SERVICE_RESPONSE, this);
        _wpt.subscribe(CoronisFrame.CMD_ERROR, this);     
        _wpt.subscribe(CoronisFrame.TR_ERROR_FRAME, this);      
        _subscribed = true;
    }
    
    public void unsubscribe() {
         if (_subscribed == false) return;
        _wpt.unsubscribe(CoronisFrame.CMD_ACK, this);
        _wpt.unsubscribe(CoronisFrame.RES_SEND_SERVICE, this);
        _wpt.unsubscribe(CoronisFrame.SERVICE_RESPONSE, this);
        _wpt.unsubscribe(CoronisFrame.CMD_ERROR, this);        
        _wpt.unsubscribe(CoronisFrame.TR_ERROR_FRAME, this);     
        _subscribed = false;
    }    
    
    public boolean process() throws IOException, CoronisException {
        
        this.subscribe();
        
        _gotACK = false;
        _gotAnswer = false;
        _isReqSendFrameError = false;
        _timeOut = false;
        _gotCmdError = false;
        _gotTransmissionError = false;
        
        _logger.debug("Timeout is : " + _allowedTime / 1000 + " seconds");
        try {
            int time = 0;
            // three retries of the same frame can be send
            for (int i =0; i < 3; i++) {
                _wpt.send(CoronisFrame.REQ_SEND_SERVICE, _message);
                time = waitForAck(time);
                if (_gotACK) break;
            } 
            if (_gotACK == false) throw new NoAckException("No ACK answer received from WavePort"); 
            do {
                if (_isReqSendFrameError == true) throw new CoronisException("Bad RES_SEND_FRAME status");
                if (_gotCmdError == true) throw new CommandException("Command error. Reprocess the request");
                if (_gotTransmissionError == true) throw new TransmissionException("Transmission error. Reprocess the request");
                if (_gotAnswer) return true;
                Thread.sleep(Request.SLEEP_TIME);
                time += Request.SLEEP_TIME;
            } while (time < _allowedTime );
             _timeOut = true;           
        } catch (InterruptedException e) {
            _logger.error("InterruptedException while waiting during PTP request");
            return false;
        }
        this.unsubscribe();
        return _gotAnswer;
    }        
    
    public void event(CoronisFrame crf) {
        switch (crf.getCmd()) {
            case CoronisFrame.CMD_ACK : 
                _gotACK = true;
                _wpt.unsubscribe(CoronisFrame.CMD_ACK, this);
                return;
            case CoronisFrame.RES_SEND_SERVICE: 
                if (((ResSendFrame)crf).getStatus() == false) {
                    // the status was not ok --> change the boolean value to false, and remove unneeded listeners
                    _isReqSendFrameError = true;
                    _wpt.unsubscribe(CoronisFrame.CMD_ERROR, this);
                    _wpt.unsubscribe(CoronisFrame.TR_ERROR_FRAME, this);                    
                }
                _wpt.unsubscribe(CoronisFrame.RES_SEND_SERVICE, this);
                return;
            case CoronisFrame.SERVICE_RESPONSE:
                String moduleDestination = ((ReceivedFrame) crf).getModuleId();
                if (moduleDestination.equals(_moduleId)) {
                    _gotAnswer = true;
                    receivedFrame = crf;                               
                    _wpt.unsubscribe(CoronisFrame.SERVICE_RESPONSE, this);    
                    _wpt.unsubscribe(CoronisFrame.CMD_ERROR, this);
                    _wpt.unsubscribe(CoronisFrame.TR_ERROR_FRAME, this);                     
                } else {
                    _logger.debug("Frame is not for me ... I am " + _moduleId + " and frame is for " + moduleDestination);
                }
                return;
            case CoronisFrame.CMD_ERROR:
                // receive a command error -> means we have to resend all the request to the waveport !
                _gotCmdError = true;
                return;
            case CoronisFrame.TR_ERROR_FRAME:
                // receive a Transmission error frame -> means we have to resend all the request to the waveport !
                try {
                	String message =((TransmissionErrorFrame)crf).getErrorMessage(); 
                    _logger.error("Trasnmission error :" + message);
                } catch (CoronisException e) {
                    _logger.error("Error while print transmission error message :" + e.getMessage());
                }
                _gotTransmissionError = true;
                return;                
        }
    }
}
