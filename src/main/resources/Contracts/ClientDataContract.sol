pragma solidity ^0.4.0;

import './Pausable.sol';


contract ClientDataContract is Pausable {

    struct ClientData {
        mapping(bytes32 => string) valueForKey;
        mapping(bytes32 => uint) allKeysIndexes; // Incremented
        bytes32[] allKeys;
    }

    mapping(uint256 => ClientData) dict;

    bytes32[] public keys;
    mapping(bytes32 => uint) public indexOfKey; // Starts from 1

    function keysCount() public constant returns(uint) {
        return keys.length;
    }

    function addKey(bytes32 key) public onlyOwner whenNotPaused {
        require(indexOfKey[key] == 0);
        keys.push(key);
        indexOfKey[key] = keys.length; // Incremented by 1
    }

    function setInfo(uint256 publicKeyX, bytes32 key, string value) public onlyOwner whenNotPaused {
        if (indexOfKey[key] == 0) {
            addKey(key);
        }

        ClientData storage clientData = dict[publicKeyX];
        if (clientData.allKeysIndexes[key] == 0) {
            clientData.allKeys.push(key);
            clientData.allKeysIndexes[key] = clientData.allKeys.length;
        }
        clientData.valueForKey[key] = value;
    }

    function deleteInfo(uint256 publicKeyX, bytes32 key) public onlyOwner whenNotPaused {
        ClientData storage clientData = dict[publicKeyX];
        uint index = clientData.allKeysIndexes[key];
        require(index > 0);
        index--;

        clientData.allKeys[index] = clientData.allKeys[clientData.allKeys.length - 1];
        clientData.allKeys.length--;
        delete clientData.allKeysIndexes[key];
        delete clientData.valueForKey[key];
        if (clientData.allKeys.length > 0) {
            clientData.allKeysIndexes[clientData.allKeys[index]] = index;
        }
    }

    function info(uint256 publicKeyX, bytes32 key) public constant returns(string) {
        return dict[publicKeyX].valueForKey[key];
    }

    function clientKeys(uint256 publicKeyX, uint index) public constant returns(bytes32) {
        return dict[publicKeyX].allKeys[index];
    }

    function clientKeysCount(uint256 publicKeyX) public constant returns(uint) {
        return dict[publicKeyX].allKeys.length;
    }

}
