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

    function get(uint256 key) public returns(uint256) {
        uint256 id = IStorageContractClient(msg.sender).storageIdentifier();
        return dict[uint256(sha3(id, key))];
    }

}

contract AccountContract is IStorageContractClient {

    mapping(uint256 => uint256) public publicKeyXById;
    mapping(uint256 => uint256) public publicKeyYById;

    uint256 constant public storageIdentifier = uint256(sha3("AccountContract"));

    function save(uint256 id, uint256 publicKeyX, uint256 publicKeyY) public {
        publicKeyXById[id] = publicKeyX;
        publicKeyYById[id] = publicKeyY;
    }

}
