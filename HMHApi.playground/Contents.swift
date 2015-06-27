//: Playground - noun: a place where people can play

import Foundation
import UIKit

enum HmhRole {
    case Administrator
    case Teacher
    case Student
}

let BASE_URL = "http://sandbox.api.hmhco.com/v1"
let API_KEY = "0708c364971bea95d01dc2fef0f6127c"
let CLIENT_ID = "9aec3c24-55ed-4121-8289-76f2cd71d15d.hmhco.com"
let CLIENT_SECRET = "cCPObRlzqQHnou1R-s9_7UjSNTDuw2oDc90oGmappC_mAeVqKCDfvuo6txE"
let USER_KEY = "c95f924dd6594edd1dd271cc9f0083ea"

func makeBodyString(params: Dictionary<String, String>) -> String {
    return join("&", map(params) {
        (k, v) in
        let escapedK = k.stringByAddingPercentEncodingWithAllowedCharacters(.URLHostAllowedCharacterSet())
        let escapedV = v.stringByAddingPercentEncodingWithAllowedCharacters(.URLHostAllowedCharacterSet())
        return escapedK! + "=" + escapedV!
    })
}

// Testing:
makeBodyString(["hello": "world", "what's": "up?"])

func makeBodyData(params: Dictionary<String, String>) -> NSData? {
    let s = makeBodyString(params)
    return s.dataUsingEncoding(NSUTF8StringEncoding, allowLossyConversion: true)
}

makeBodyData(["hello": "world", "what's": "up?"])


class HmhUser {
    let api: HmhAPI
        let authorization: String
    let json: [String: AnyObject]
    
    var roles: String? {
        get {
            return json["roles"] as? String
        }
    }
    
    var username: String? {
        get {
            return json["preferred_username"] as? String
        }
    }
    
    var name: String? {
        get {
            return json["name"] as? String
        }
    }
    
    init(api: HmhAPI, json: [String: AnyObject]) {
        self.api = api
        self.json = json
        authorization = json["access_token"] as! String
    }
    
    func makeAuthorizedCall(req: NSMutableURLRequest, params: [String: AnyObject]?, method: String = "POST") -> [String: AnyObject]? {
        req.addValue(authorization, forHTTPHeaderField: "Authorization")
        return self.api.makeCall(req)
    }
    
    func getSections() -> Void {
            
    }
    
    func getAssignments() -> Void {
        
    }
}


class HmhAPI {
    let apiKey: String
    let clientId: String
    let clientSecret: String
    let userKey: String
    
    init(apiKey: String=API_KEY, clientId: String=CLIENT_ID,
        clientSecret: String=CLIENT_SECRET, userKey: String=USER_KEY) {

            self.apiKey = apiKey
            self.clientId = clientId
            self.clientSecret = clientSecret
            self.userKey = userKey
    }
    
    
    func makeCall(req: NSMutableURLRequest) -> [String: AnyObject]? {
        var response: NSURLResponse?
        var error: NSError?

        let responseData = NSURLConnection.sendSynchronousRequest(req, returningResponse: &response, error: &error)
        
        if let data = responseData {
            var error: NSError?
            let jsonObject: AnyObject? = NSJSONSerialization.JSONObjectWithData(data, options: .allZeros, error: &error)
            
            if let json = jsonObject as? [String: AnyObject] {
                return json
            }
        }
        
        return nil
    }
    
    func makeRequest(url: String, params: Dictionary<String, String>? = nil, method: String = "POST") -> NSMutableURLRequest {
        let req = NSMutableURLRequest(URL: NSURL(string: url)!)
        req.addValue(self.userKey, forHTTPHeaderField: "Vnd-HMH-Api-Key")
        req.HTTPMethod = method
        
        if let params = params {
            if method == "GET" {
                let qstring = makeBodyString(params)
                req.URL = NSURL(string: "\(url)?\(qstring)")
            } else {
                if let data = makeBodyData(params) {
                    req.HTTPBody = data
                }
            }
        }
        return req
    }
    
    func authorizeUser(user: String, password: String) -> HmhUser? {
        let req = makeRequest("\(BASE_URL)/sample_token/", params: ["client_id": clientId, "grant_type": "password", "username": user, "password": password])
        var responseData: NSData?
        
        if let json = makeCall(req) {
            return HmhUser(api: self, json: json)
        }
        
        return nil
    }
}

let api = HmhAPI()
if let user = api.authorizeUser("sauron", password: "password") {
    print(user.name)
    print(user.roles)
}
//
//
//class HmhDocument {
//    
//}
//
