pragma solidity ^0.4.0;

//import './src/main/resources/StorageContract.sol';

contract Ownable {
    address public owner;

    event OwnershipTransferred(address indexed previousOwner, address indexed newOwner);

    function Ownable() public {
        owner = msg.sender;
    }

    modifier onlyOwner() {
        require(msg.sender == owner);
        _;
    }

    function transferOwnership(address newOwner) public onlyOwner {
        require(newOwner != address(0));
        OwnershipTransferred(owner, newOwner);
        owner = newOwner;
    }
}

//

contract IStorageContractClient {

    uint256 public storageIdentifier;

}

contract StorageContract {

    mapping (uint256 => uint256) public dict;

    function set(uint256 key, uint256 value) public {
        uint256 id = IStorageContractClient(msg.sender).storageIdentifier();
        dict[uint256(keccak256(id, key))] = value;
    }

    function erase(uint256 key) public {
        uint256 id = IStorageContractClient(msg.sender).storageIdentifier();
        delete dict[uint256(keccak256(id, key))];
    }

    function get(uint256 key) public constant returns(uint256) {
        uint256 id = IStorageContractClient(msg.sender).storageIdentifier();
        return dict[uint256(keccak256(id, key))];
    }

    function length(uint256 key) public constant returns(uint) {
        uint256 id = IStorageContractClient(msg.sender).storageIdentifier();
        uint len = 0;
        while (dict[uint256(keccak256(id, key + len))] != 0) {
            len++;
        }
        return len;
    }

}

contract AccountContract is Ownable, IStorageContractClient {

    StorageContract public storageContract;

    uint256 constant public storageIdentifier = uint256(keccak256("AccountContract"));
    uint256 constant public publicKeyField = uint256(keccak256("publicKeyField"));

    function AccountContract(StorageContract _storageContract) public {
        storageContract = (_storageContract != address(0)) ? _storageContract : new StorageContract();
    }

    function isRegisteredPublicKey(string publicKey) public constant returns(bool) {
        return storageContract.get(uint256(keccak256(publicKeyField, publicKey))) != 0;
    }

    function registerPublicKey(string publicKey) public onlyOwner {
        require(!isRegisteredPublicKey(publicKey));
        storageContract.set(uint256(keccak256(publicKeyField, publicKey)), 1);
    }

}

contract ClientDataContract is Ownable, IStorageContractClient {

    StorageContract public storageContract;

    uint256 constant public storageIdentifier = uint256(keccak256("ClientDataContract"));
    uint256 constant public infoField = uint256(keccak256("infoField"));

    bytes32[] public keys;
    mapping(bytes32 => uint) public indexOfKey; // Starts from 1

    function keysCount() public constant returns(uint) {
        return keys.length;
    }

    function addKey(bytes32 key) public onlyOwner {
        require(indexOfKey[key] == 0);
        keys.push(key);
        indexOfKey[key] = keys.length; // Incremented by 1
    }

    function ClientDataContract(StorageContract _storageContract) public {
        storageContract = (_storageContract != address(0)) ? _storageContract : new StorageContract();
    }

    function infoLength(string publicKey, bytes32 key) public constant returns(uint) {
        return storageContract.length(uint256(keccak256(publicKey, infoField, key)));
    }

    function info(string publicKey, bytes32 key, uint index) public constant returns(bytes32) {
        return bytes32(storageContract.get(uint256(keccak256(publicKey, infoField, key)) + index));
    }

    function setInfo(string publicKey, bytes32 key, uint index, bytes32 value) public onlyOwner {
        if (indexOfKey[key] == 0) {
            addKey(key);
        }
        storageContract.set(uint256(keccak256(publicKey, infoField, key)) + index, uint256(value));
    }

    function setInfos(string publicKey, bytes32 key, bytes32[] values) public onlyOwner {
        if (indexOfKey[key] == 0) {
            addKey(key);
        }
        for (uint i = 0; i < values.length; i++) {
            storageContract.set(uint256(keccak256(publicKey, infoField, key)) + i, uint256(values[i]));
        }
        storageContract.erase(uint256(keccak256(publicKey, infoField, key)) + values.length);
    }

}

contract RequestDataContract is Ownable, IStorageContractClient {

    StorageContract public storageContract;

    uint256 constant public storageIdentifier = uint256(keccak256("RequestDataContract"));

    enum RequestDataState {
        UNDEFINED,
        AWAIT,
        ACCEPT,
        REJECT
    }

    struct RequestData {
        uint id;
        uint256 fromPkX;
        uint256 fromPkY;
        uint256 toPkX;
        uint256 toPkY;
        bytes requestData;
        bytes responseData;
        RequestDataState state;
    }

    function RequestDataContract(StorageContract _storageContract) public {
        storageContract = (_storageContract != address(0)) ? _storageContract : new StorageContract();
    }

    uint public nextId = 1;
    RequestData[] public requests;
    mapping(uint => uint) public indexOfRequestId; // Incremented values
    event RequestDataCreated(uint indexed requestId);
    event RequestDataAccepted(uint indexed requestId);
    event RequestDataRejected(uint indexed requestId);

    struct ItemsAndLookupEntry {
        uint[] items;
        mapping(uint => uint) lookup;
    }

    mapping(uint256 => mapping(uint => ItemsAndLookupEntry)) idsByFrom; // [PkX][state]
    mapping(uint256 => mapping(uint => ItemsAndLookupEntry)) idsByTo; // [PkX][state]
    mapping(uint256 => mapping(uint256 => mapping(uint => ItemsAndLookupEntry))) idsByFromAndTo; // [PkX][PkX][state]

    // Lengths

    function requestsCount() public constant returns(uint) {
        return requests.length;
    }

    function getByFromCount(uint256 fromPkX, uint state) public constant returns(uint) {
        return idsByFrom[fromPkX][state].items.length;
    }

    function getByToCount(uint256 toPkX, uint state) public constant returns(uint) {
        return idsByTo[toPkX][state].items.length;
    }

    function getByFromAndToCount(uint256 fromPkX, uint256 toPkX, uint state) public constant returns(uint) {
        return idsByFromAndTo[fromPkX][toPkX][state].items.length;
    }

    // Public methods

    function getByFrom(uint256 fromPkX, uint state, uint index) public constant returns(uint) {
        return idsByFrom[fromPkX][state].items[index];
    }

    function getByTo(uint256 toPkX, uint state, uint index) public constant returns(uint) {
        return idsByTo[toPkX][state].items[index];
    }

    function getByFromAndTo(uint256 fromPkX, uint256 toPkX, uint state, uint index) public constant returns(uint) {
        return idsByFromAndTo[fromPkX][toPkX][state].items[index];
    }

    function findById(uint id) public constant
        returns(uint request_id,
                uint256 fromPkX,
                uint256 fromPkY,
                uint256 toPkX,
                uint256 toPkY,
                bytes requestData,
                bytes responseData,
                uint state)
    {
        uint index = indexOfRequestId[id];
        require(index > 0);
        index--;
        RequestData storage data = requests[index];
        return (data.id, data.fromPkX, data.fromPkY, data.toPkX, data.toPkY, data.requestData, data.responseData, uint(data.state));
    }

    function updateData(
        uint id,
        uint256 fromPkX,
        uint256 fromPkY,
        uint256 toPkX,
        uint256 toPkY,
        bytes requestData,
        bytes responseData,
        uint state) public onlyOwner
    {
        //require(isValidPublicKey(fromPkX, fromPkY));
        //require(isValidPublicKey(toPkX, toPkY));

        if (id == 0) {
            id = nextId++;
            requests.push(RequestData(id, fromPkX, fromPkY, toPkX, toPkY, requestData, responseData, RequestDataState(state)));
            indexOfRequestId[id] = requests.length; // Incremented index
            RequestDataCreated(id);
            return;
        }

        uint index = indexOfRequestId[id];
        require(index > 0);
        index--;

        uint oldState = uint(requests[index].state);
        if (state != oldState) {
            moveId(id, idsByFrom[fromPkX][oldState], idsByFrom[fromPkX][state]);
            moveId(id, idsByTo[toPkX][oldState], idsByTo[toPkX][state]);
            moveId(id, idsByFromAndTo[fromPkX][toPkX][oldState], idsByFromAndTo[fromPkX][toPkX][state]);
            if (state == uint(RequestDataState.ACCEPT)) {
                RequestDataAccepted(id);
            } else
            if (state == uint(RequestDataState.REJECT)) {
                RequestDataRejected(id);
            }
        }

        requests[index] = RequestData(id, fromPkX, fromPkY, toPkX, toPkY, requestData, responseData, RequestDataState(state));
    }

    // Private methods

    function isValidPublicKey(uint256 pkX, uint256 pkY) public constant returns(bool) {
        // (y^2 == x^3 + 7) mod m
        uint256 m = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F;
        return mulmod(pkY, pkY, m) == addmod(mulmod(pkX, mulmod(pkX, pkX, m), m), 7, m);
    }

    function moveId(uint id, ItemsAndLookupEntry storage fromEntry, ItemsAndLookupEntry storage toEntry) internal {
        uint index = fromEntry.lookup[id];
        require(index > 0);
        index--;

        delete fromEntry.lookup[id];
        fromEntry.items[index] = fromEntry.items[fromEntry.items.length - 1];
        fromEntry.items.length--;
        fromEntry.lookup[fromEntry.items[index]] = index + 1; // Incremented index

        toEntry.items.push(id);
        toEntry.lookup[id] = toEntry.items.length; // Incremented index
    }

}

contract NameServiceContract is Ownable {

    string[] public names;
    mapping(bytes32 => uint) private indexOfNameHash; // Incremented indexes
    mapping(bytes32 => address) private addressOfNameHash;

    function incrementedIndexOfName(string name) public constant returns(uint) {
        return indexOfNameHash[keccak256(name)];
    }

    function addressOfName(string name) public constant returns(address) {
        return addressOfNameHash[keccak256(name)];
    }

    function setAddressOf(string name, address addr) public onlyOwner {
        bytes32 nameHash = keccak256(name);
        if (indexOfNameHash[nameHash] == 0) {
            names.push(name);
            indexOfNameHash[nameHash] = names.length; // Incremented
        }
        addressOfNameHash[nameHash] = addr;
    }

}