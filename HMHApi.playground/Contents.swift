//: Playground - noun: a place where people can play

import Foundation
import UIKit

enum HmhRole {
    case Administrator
    case Teacher
    case Student
}

enum HmhAssignmentStatus: String {
    case InProgress = "IN_PROGRESS"
    case TurnedIn = "TURNED_IN"
}

struct HmhSection {
    let refId: String
    let name: String
}

let BASE_URL = "http://sandbox.api.hmhco.com/v1"
let API_KEY = "0708c364971bea95d01dc2fef0f6127c"
let CLIENT_ID = "9aec3c24-55ed-4121-8289-76f2cd71d15d.hmhco.com"
let CLIENT_SECRET = "cCPObRlzqQHnou1R-s9_7UjSNTDuw2oDc90oGmappC_mAeVqKCDfvuo6txE"
let USER_KEY = "b31eaf0f10bebab88ee1fb61f7a39954"


func makeBodyString(params: Dictionary<String, String>) -> String {
    return join("&", map(params) {
        (k, v) in
        let escapedK = k.stringByAddingPercentEncodingWithAllowedCharacters(.URLHostAllowedCharacterSet())
        let escapedV = v.stringByAddingPercentEncodingWithAllowedCharacters(.URLHostAllowedCharacterSet())
        return escapedK! + "=" + escapedV!
    })
}

func makeBodyData(params: Dictionary<String, String>) -> NSData? {
    let s = makeBodyString(params)
    return s.dataUsingEncoding(NSUTF8StringEncoding, allowLossyConversion: true)
}

func formatDate(date: NSDate) -> String {
    let formatter = NSDateFormatter()
    formatter.timeStyle = .NoStyle
    formatter.dateFormat = "yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'"
    return formatter.stringFromDate(date)
}

func parseDate(date: String) -> NSDate! {
    let formatter = NSDateFormatter()
    formatter.timeStyle = .NoStyle
    formatter.dateFormat = "yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'"
    
    return formatter.dateFromString(date)
}


class HmhUser {
    let api: HmhAPI
    let authorization: String
    let json: [String: AnyObject]
    
    var roles: String? {
        get {
            return json["roles"] as? String
        }
    }
    
    var username: String {
        get {
            return json["preferred_username"] as! String
        }
    }
    
    var name: String {
        get {
            return json["name"] as! String
        }
    }
    
    var refId: String {
        get {
            return json["ref_id"] as! String
        }
    }
    
    init(api: HmhAPI, json: [String: AnyObject]) {
        self.api = api
        self.json = json
        print(json)
        authorization = json["access_token"] as! String
//        print(authorization)
    }
    
    func makeRequest(url: String, params: Dictionary<String, String>? = nil, method: String = "GET") -> NSMutableURLRequest  {
        return self.api.makeRequest(url, params: params, method: method)
    }
    
    func makeAuthorizedCall(req: NSMutableURLRequest) -> AnyObject? {
        req.addValue(authorization, forHTTPHeaderField: "Authorization")

        return api.makeCall(req)
    }
    
    func getAssignments() -> Void {
        let req = makeRequest("\(BASE_URL)/assignments")
        let jsonObject = makeAuthorizedCall(req)
        if let json = makeAuthorizedCall(req) as? [AnyObject] {
            print(json)
        }
    }
    
    func updateAssignmentStatus(refId: String, status: HmhAssignmentStatus) {
        makeRequest("\(BASE_URL)/assignmentSubmissions/\(refId)", params: nil, method: "PATCH")
    }
    
    func createAssignments(name: String, dueDate: NSDate, availableDate: NSDate = NSDate()) -> Void {
        let dueDateString = formatDate(dueDate)
        let availableDateString = formatDate(availableDate)
        
        var assignment = [
            "schoolRefId": "",
            "sectionRefId": "",
            "students": [],
            "availableDate": availableDateString,
            "dueDate": dueDateString,
            "name": "Something",
            "description": "Something else",
            "creatorRefId": refId]
        var error: NSError?
        let assignmentJSON = NSJSONSerialization.dataWithJSONObject(assignment, options: .allZeros, error: &error)
        
        let req = makeRequest("\(BASE_URL)/assignments", method: "POST")
        req.HTTPBody = assignmentJSON
        makeAuthorizedCall(req)
    }
    
    func getAssignmentSubmissions() -> Void {
        let req = makeRequest("\(BASE_URL)/assignments")
        if let json = makeAuthorizedCall(req) as? [String: AnyObject] {
            print(json)
        }
    }
    
    func getStudentData() -> Void {
        let req = makeRequest("\(BASE_URL)/students")
        if let json = makeAuthorizedCall(req) as? [AnyObject] {
            print(json)
        }
//        makeAuthorizedCall(req, params: <#[String : AnyObject]?#>, method: <#String#>)
    }
    
    func getSections() -> [AnyObject]? {
        let req = makeRequest("\(BASE_URL)/sections")
        if let json = makeAuthorizedCall(req) as? [AnyObject] {
            print(json)
            return json
        }
        
        return nil
    }
    
    func getSectionsForStaff(refId: String) -> [AnyObject?] {
        if let sections = getSections() {
            return filter(sections) {
                (section) in
                return true
            }
        }
        
        return []
    }
    
    func getStudentIdsForSection(refId: String) -> [String] {
        let req = makeRequest("\(BASE_URL)/studentSectionAssociations")
        if let json = makeAuthorizedCall(req) as? [AnyObject] {
            print(json)
//            map(filter(json) {
//                (obj) in
//            }
        }
        
        return []
    }
}

class HmhSubmission {
    let user: HmhUser
    
    var api: HmhAPI! {
        get {
            return user.api
        }
    }
    
    init(user: HmhUser, json: [String: AnyObject?]) {
        self.user = user
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
    
    
    func makeCall(req: NSMutableURLRequest) -> AnyObject? {
        var response: NSURLResponse?
        var error: NSError?

        let responseData = NSURLConnection.sendSynchronousRequest(req, returningResponse: &response, error: &error)
        
        if let data = responseData {
            var error: NSError?
            let jsonObject: AnyObject? = NSJSONSerialization.JSONObjectWithData(data, options: .allZeros, error: &error)
            let s = NSString(data: data, encoding: NSUTF8StringEncoding)
//            print("Response body:", s!)
            
            if let error = error {
                print("Error while unserializing:")
                print(error)
            }
            
            return jsonObject
        }
        
        return nil
    }
    
    func makeRequest(url: String, params: Dictionary<String, String>? = nil, method: String = "GET") -> NSMutableURLRequest {
        let req = NSMutableURLRequest(URL: NSURL(string: url)!)
        req.addValue(self.userKey, forHTTPHeaderField: "Vnd-HMH-Api-Key")
        req.addValue("application/json", forHTTPHeaderField: "Accept")
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
        let req = makeRequest("\(BASE_URL)/sample_token/", params: ["client_id": clientId, "grant_type": "password", "username": user, "password": password], method: "POST")
        var responseData: NSData?
        
        if let json = makeCall(req) as? [String: AnyObject] {
            return HmhUser(api: self, json: json)
        }
        
        return nil
    }
}


let api = HmhAPI()
if let user = api.authorizeUser("sauron", password: "password") {
    print(user.name)
    print(user.roles)
    user.getAssignments()
    user.getStudentData()
    user.getSections()
}
//
//
//class HmhDocument {
//    
//}
//
