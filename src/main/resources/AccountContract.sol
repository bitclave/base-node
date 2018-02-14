pragma solidity ^0.4.0;

//import './src/main/resources/StorageContract.sol';

contract IStorageContractClient {

    uint256 public storageIdentifier;

}

contract StorageContract {

    mapping (uint256 => uint256) public dict;

    function set(uint256 key, uint256 value) public {
        uint256 id = IStorageContractClient(msg.sender).storageIdentifier();
        dict[uint256(sha3(id, key))] = value;
    }

    function erase(uint256 key) public {
        uint256 id = IStorageContractClient(msg.sender).storageIdentifier();
        delete dict[uint256(sha3(id, key))];
    }

    function get(uint256 key) public returns(uint256) {
        uint256 id = IStorageContractClient(msg.sender).storageIdentifier();
        return dict[uint256(sha3(id, key))];
    }

    function length(uint256 key) public returns(uint) {
        uint256 id = IStorageContractClient(msg.sender).storageIdentifier();
        uint length = 0;
        while (dict[uint256(sha3(id, key + length))] != 0) {
            length++;
        }
        return length;
    }

}

contract AccountContract is IStorageContractClient {

    StorageContract public storageContract;

    uint256 constant public storageIdentifier = uint256(sha3("AccountContract"));
    uint256 constant public publicKeyField = uint256(sha3("publicKeyField"));
    uint256 constant public infoField = uint256(sha3("infoField"));

    function AccountContract(StorageContract _storageContract) public {
        storageContract = (_storageContract != address(0)) ? _storageContract : new StorageContract();
    }

    function isRegisteredPublicKey(string publicKey) public constant returns(bool) {
        return storageContract.get(uint256(sha3(publicKeyField, publicKey))) != 0;
    }

    function registerPublicKey(string publicKey) public {
        storageContract.set(uint256(sha3(publicKeyField, publicKey)), 1);
    }

    function infoLength(string publicKey, uint256 hash) public constant returns(uint) {
        return storageContract.length(uint256(sha3(publicKey, infoField, hash)));
    }

    function info(string publicKey, uint256 hash, uint index) public constant returns(bytes32) {
        return bytes32(storageContract.get(uint256(sha3(publicKey, infoField, hash)) + index));
    }

    function setInfo(string publicKey, uint256 hash, uint index, bytes32 value) public {
        storageContract.set(uint256(sha3(publicKey, infoField, hash)) + index, uint256(value));
    }

    function setInfos(string publicKey, uint256 hash, bytes32[] values) public {
        for (uint i = 0; i < values.length; i++) {
            storageContract.set(uint256(sha3(publicKey, infoField, hash)) + i, uint256(values[i]));
        }
        storageContract.erase(uint256(sha3(publicKey, infoField, hash)) + values.length);
    }

}
